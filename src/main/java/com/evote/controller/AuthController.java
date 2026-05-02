package com.evote.controller;

import com.evote.model.Voter;
import com.evote.service.ElectionService;
import com.evote.service.EmailService;
import com.evote.service.FaceVerificationService;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class AuthController {

    private final ElectionService         svc;
    private final FaceVerificationService faceService;
    private final EmailService            emailService;

    public AuthController(ElectionService svc,
                          FaceVerificationService faceService,
                          EmailService emailService) {
        this.svc          = svc;
        this.faceService  = faceService;
        this.emailService = emailService;
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    @GetMapping({"/", "/login"})
    public String loginPage(HttpSession session, Model model,
                            @RequestParam(required = false) String success) {
        Object userId = session.getAttribute("userId");
        if (userId != null) {
            String role = (String) session.getAttribute("role");
            if ("admin".equals(role)) return "redirect:/admin/dashboard";
            if ("voter".equals(role)) return "redirect:/voter/dashboard";
        }
        if ("registered".equals(success)) model.addAttribute("success", "Account created! You can now sign in.");
        if ("reset".equals(success))      model.addAttribute("success", "Password reset successfully!");
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session, Model model) {
        if (username.isBlank() || password.isBlank()) {
            model.addAttribute("error", "Please fill in all fields.");
            return "login";
        }
        if ("admin".equals(username) && "admin123".equals(password)) {
            session.setAttribute("userId",   "admin");
            session.setAttribute("userName", "Admin");
            session.setAttribute("role",     "admin");
            return "redirect:/admin/dashboard";
        }
        Optional<Voter> voter = svc.authenticateVoter(username, password);
        if (voter.isPresent()) {
            session.setAttribute("userId",   voter.get().getVoterId());
            session.setAttribute("userName", voter.get().getName());
            session.setAttribute("hasVoted", voter.get().isHasVoted());
            session.setAttribute("role",     "voter");
            return "redirect:/voter/dashboard";
        }
        model.addAttribute("error", "Invalid username or password.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ── Register — Step 1: show form ──────────────────────────────────────────
    @GetMapping("/register")
    public String registerPage() { return "register"; }

    // ── Register — Step 2: send OTP ───────────────────────────────────────────
    @PostMapping("/register/send-otp")
    public String sendOtp(
            // Personal
            @RequestParam(required=false) String firstName,
            @RequestParam(required=false) String middleName,
            @RequestParam(required=false) String lastName,
            @RequestParam(required=false) String dateOfBirth,
            @RequestParam(required=false) String gender,
            // Address
            @RequestParam(required=false) String street,
            @RequestParam(required=false) String barangay,
            @RequestParam(required=false) String city,
            @RequestParam(required=false) String province,
            @RequestParam(required=false) String zipCode,
            // Contact
            @RequestParam(required=false) String mobileNumber,
            @RequestParam(required=false) String email,
            // Voter info
            @RequestParam(required=false) String voterIdNumber,
            @RequestParam(required=false) String votingDistrict,
            @RequestParam(required=false) String affiliation,
            @RequestParam(required=false) String idType,
            @RequestParam(required=false) String idNumber,
            // Account
            @RequestParam String voterId,
            @RequestParam String name,
            @RequestParam String password,
            @RequestParam String confirm,
            HttpSession session, Model model) {

        if (voterId == null || voterId.isBlank()) {
            model.addAttribute("error", "Voter ID is required."); return "register";
        }
        if (email == null || email.isBlank()) {
            model.addAttribute("error", "Email address is required."); return "register";
        }
        if (password == null || password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters."); return "register";
        }
        if (!password.equals(confirm)) {
            model.addAttribute("error", "Passwords do not match."); return "register";
        }
        if (svc.voterIdExists(voterId)) {
            model.addAttribute("error", "Voter ID '" + voterId + "' is already taken."); return "register";
        }

        // Store all form data in session
        session.setAttribute("reg_voterId",       voterId);
        session.setAttribute("reg_name",          name);
        session.setAttribute("reg_firstName",     firstName);
        session.setAttribute("reg_middleName",    middleName);
        session.setAttribute("reg_lastName",      lastName);
        session.setAttribute("reg_dateOfBirth",   dateOfBirth);
        session.setAttribute("reg_gender",        gender);
        session.setAttribute("reg_street",        street);
        session.setAttribute("reg_barangay",      barangay);
        session.setAttribute("reg_city",          city);
        session.setAttribute("reg_province",      province);
        session.setAttribute("reg_zipCode",       zipCode);
        session.setAttribute("reg_mobileNumber",  mobileNumber);
        session.setAttribute("reg_email",         email);
        session.setAttribute("reg_voterIdNumber", voterIdNumber);
        session.setAttribute("reg_votingDistrict",votingDistrict);
        session.setAttribute("reg_affiliation",   affiliation);
        session.setAttribute("reg_idType",        idType);
        session.setAttribute("reg_idNumber",      idNumber);
        session.setAttribute("reg_password",      password);

        // Generate and send OTP
        String otp = emailService.generateOtp(voterId);
        String emailError = null;
        try {
            emailService.sendOtpEmail(email, name, otp);
        } catch (Exception e) {
            emailError = e.getMessage();
            System.err.println("OTP email failed: " + e.getMessage());
        }
        session.setAttribute("reg_otp_fallback", emailError != null ? otp : null);

        return "redirect:/register/verify-otp";
    }

    // ── Register — Step 3: verify OTP ────────────────────────────────────────
    @GetMapping("/register/verify-otp")
    public String otpPage(HttpSession session, Model model) {
        if (session.getAttribute("reg_voterId") == null) return "redirect:/register";
        model.addAttribute("email", session.getAttribute("reg_email"));
        // Show fallback OTP on screen if email failed
        String fallback = (String) session.getAttribute("reg_otp_fallback");
        if (fallback != null) {
            model.addAttribute("otpFallback", fallback);
        }
        return "otp-verify";
    }

    @PostMapping("/register/verify-otp")
    public String verifyOtp(@RequestParam String otp,
                            HttpSession session, Model model) {
        String voterId = (String) session.getAttribute("reg_voterId");
        if (voterId == null) return "redirect:/register";

        if (!emailService.verifyOtp(voterId, otp)) {
            model.addAttribute("error", "Invalid or expired OTP. Please try again.");
            model.addAttribute("email", session.getAttribute("reg_email"));
            return "otp-verify";
        }

        session.setAttribute("otp_verified", true);
        return "redirect:/register/complete";
    }

    // ── Register — Step 4: complete with ID + face ────────────────────────────
    @GetMapping("/register/complete")
    public String completePage(HttpSession session) {
        if (!Boolean.TRUE.equals(session.getAttribute("otp_verified"))) return "redirect:/register";
        return "register-complete";
    }

    @PostMapping("/register/complete")
    public String doComplete(@RequestParam(required = false) MultipartFile idPhoto,
                             @RequestParam(required = false) String selfieData,
                             HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute("otp_verified"))) return "redirect:/register";

        String voterId       = (String) session.getAttribute("reg_voterId");
        String name          = (String) session.getAttribute("reg_name");
        String email         = (String) session.getAttribute("reg_email");
        String password      = (String) session.getAttribute("reg_password");
        String firstName     = (String) session.getAttribute("reg_firstName");
        String middleName    = (String) session.getAttribute("reg_middleName");
        String lastName      = (String) session.getAttribute("reg_lastName");
        String dob           = (String) session.getAttribute("reg_dateOfBirth");
        String gender        = (String) session.getAttribute("reg_gender");
        String street        = (String) session.getAttribute("reg_street");
        String barangay      = (String) session.getAttribute("reg_barangay");
        String city          = (String) session.getAttribute("reg_city");
        String province      = (String) session.getAttribute("reg_province");
        String zipCode       = (String) session.getAttribute("reg_zipCode");
        String mobile        = (String) session.getAttribute("reg_mobileNumber");
        String voterIdNum    = (String) session.getAttribute("reg_voterIdNumber");
        String district      = (String) session.getAttribute("reg_votingDistrict");
        String affiliation   = (String) session.getAttribute("reg_affiliation");
        String idTypeS       = (String) session.getAttribute("reg_idType");
        String idNumberS     = (String) session.getAttribute("reg_idNumber");

        // Face verification
        if (idPhoto != null && !idPhoto.isEmpty() && selfieData != null && !selfieData.isBlank()) {
            try {
                String base64      = selfieData.contains(",") ? selfieData.split(",")[1] : selfieData;
                byte[] selfieBytes = java.util.Base64.getDecoder().decode(base64);
                byte[] idBytes     = idPhoto.getBytes();
                FaceVerificationService.FaceCompareResult result = faceService.compareFaces(idBytes, selfieBytes);
                if (!result.passed) {
                    model.addAttribute("error", "Face verification failed: " + result.message);
                    return "register-complete";
                }
            } catch (Exception e) {
                model.addAttribute("error", "Face verification error: " + e.getMessage());
                return "register-complete";
            }
        }

        // Use session name as full name fallback
        String fn = firstName != null ? firstName : name;
        String ln = lastName  != null ? lastName  : "";

        svc.addVoterFull(voterId, fn, middleName, ln, dob, gender,
            street, barangay, city, province, zipCode, mobile, email,
            voterIdNum, district, affiliation, idTypeS, idNumberS, password);

        // Clear session
        String[] keys = {"reg_voterId","reg_name","reg_firstName","reg_middleName","reg_lastName",
            "reg_dateOfBirth","reg_gender","reg_street","reg_barangay","reg_city","reg_province",
            "reg_zipCode","reg_mobileNumber","reg_email","reg_voterIdNumber","reg_votingDistrict",
            "reg_affiliation","reg_idType","reg_idNumber","reg_password","otp_verified","reg_otp_fallback"};
        for (String k : keys) session.removeAttribute(k);

        return "redirect:/login?success=registered";
    }

    // ── Forgot password (OTP-based) ───────────────────────────────────────────
    @GetMapping("/forgot-password")
    public String forgotPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String doForgot(@RequestParam String step,
                           @RequestParam(required = false) String voterId,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String otp,
                           @RequestParam(required = false) String newPassword,
                           @RequestParam(required = false) String confirm,
                           Model model) {
        switch (step) {

            case "request" -> {
                // Find voter by ID and verify email matches
                if (voterId == null || voterId.isBlank() || email == null || email.isBlank()) {
                    model.addAttribute("error", "Please fill in all fields.");
                    return "forgot-password";
                }
                Optional<com.evote.model.Voter> voter = svc.findVoterByIdAndEmail(voterId, email);
                if (voter.isEmpty()) {
                    model.addAttribute("error", "No account found with that Voter ID and email combination.");
                    return "forgot-password";
                }
                // Send OTP
                String generatedOtp = emailService.generateOtp(voterId);
                String fallback = null;
                try {
                    emailService.sendOtpEmail(email, voter.get().getName(), generatedOtp);
                } catch (Exception e) {
                    fallback = generatedOtp;
                    System.err.println("Reset OTP email failed: " + e.getMessage());
                }
                // Mask email for display
                String masked = email.replaceAll("(?<=.{2}).(?=.*@)", "*");
                model.addAttribute("step",        "otp");
                model.addAttribute("voterId",     voterId);
                model.addAttribute("maskedEmail", masked);
                if (fallback != null) model.addAttribute("otpFallback", fallback);
            }

            case "verify-otp" -> {
                if (!emailService.verifyOtp(voterId, otp)) {
                    model.addAttribute("error",   "Invalid or expired code. Please try again.");
                    model.addAttribute("step",    "otp");
                    model.addAttribute("voterId", voterId);
                    return "forgot-password";
                }
                model.addAttribute("step",    "reset");
                model.addAttribute("voterId", voterId);
            }

            case "reset" -> {
                if (newPassword == null || newPassword.length() < 6) {
                    model.addAttribute("error",   "Password must be at least 6 characters.");
                    model.addAttribute("step",    "reset");
                    model.addAttribute("voterId", voterId);
                    return "forgot-password";
                }
                if (!newPassword.equals(confirm)) {
                    model.addAttribute("error",   "Passwords do not match.");
                    model.addAttribute("step",    "reset");
                    model.addAttribute("voterId", voterId);
                    return "forgot-password";
                }
                svc.updatePassword(voterId, newPassword);
                return "redirect:/login?success=reset";
            }
        }
        return "forgot-password";
    }
}
