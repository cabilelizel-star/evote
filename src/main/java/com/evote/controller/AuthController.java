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

    public AuthController(ElectionService svc, FaceVerificationService faceService, EmailService emailService) {
        this.svc = svc; this.faceService = faceService; this.emailService = emailService;
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
        if ("registered".equals(success)) model.addAttribute("success", "Registration submitted! Awaiting admin approval.");
        if ("reset".equals(success))      model.addAttribute("success", "Password reset successfully!");
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username, @RequestParam String password,
                          HttpSession session, Model model) {
        if (username.isBlank() || password.isBlank()) {
            model.addAttribute("error", "Please fill in all fields."); return "login";
        }
        if ("Admin".equals(username) && "Admin123".equals(password)) {
            session.setAttribute("userId",   "admin");
            session.setAttribute("userName", "Admin");
            session.setAttribute("role",     "admin");
            return "redirect:/admin/dashboard";
        }
        Optional<Voter> voter = svc.authenticateVoter(username, password);
        if (voter.isPresent()) {
            Voter v = voter.get();
            // Check approval status
            if ("rejected".equals(v.getStatus())) {
                model.addAttribute("error", "Your registration was rejected. Reason: " +
                    (v.getRejectionReason() != null ? v.getRejectionReason() : "Contact admin."));
                return "login";
            }
            if (v.isPending()) {
                model.addAttribute("error", "Your account is pending admin approval. Please wait.");
                return "login";
            }
            session.setAttribute("userId",   v.getVoterId());
            session.setAttribute("userName", v.getName());
            session.setAttribute("hasVoted", v.isHasVoted());
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

    // ── Register ──────────────────────────────────────────────────────────────
    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register/otp/send")
    @ResponseBody
    public java.util.Map<String,String> sendOtpAjax(@RequestParam String voterId,
                                                     @RequestParam String email,
                                                     @RequestParam String name) {
        try {
            if (svc.voterIdExists(voterId))
                return java.util.Map.of("status","error","message","Voter ID '" + voterId + "' is already taken.");
            String otp = emailService.generateOtp(voterId);
            try {
                emailService.sendOtpEmail(email, name, otp);
                return java.util.Map.of("status","sent","message","OTP sent to " + email);
            } catch (Exception e) {
                return java.util.Map.of("status","fallback","otp",otp,"message","Email failed.");
            }
        } catch (Exception e) {
            return java.util.Map.of("status","error","message",e.getMessage());
        }
    }

    @PostMapping("/register/otp/verify")
    @ResponseBody
    public java.util.Map<String,String> verifyOtpAjax(@RequestParam String voterId,
                                                       @RequestParam String otp) {
        return emailService.verifyOtp(voterId, otp)
            ? java.util.Map.of("status","ok")
            : java.util.Map.of("status","error","message","Invalid or expired code.");
    }

    @PostMapping("/register/submit")
    public String submitRegister(
            @RequestParam(required=false) String firstName,
            @RequestParam(required=false) String middleName,
            @RequestParam(required=false) String lastName,
            @RequestParam(required=false) String dateOfBirth,
            @RequestParam(required=false) String gender,
            @RequestParam(required=false) String street,
            @RequestParam(required=false) String barangay,
            @RequestParam(required=false) String city,
            @RequestParam(required=false) String province,
            @RequestParam(required=false) String zipCode,
            @RequestParam(required=false) String mobileNumber,
            @RequestParam(required=false) String email,
            @RequestParam(required=false) String voterIdNumber,
            @RequestParam(required=false) String votingDistrict,
            @RequestParam(required=false) String affiliation,
            @RequestParam(required=false) String idType,
            @RequestParam(required=false) String idNumber,
            @RequestParam(required=false) MultipartFile idPhoto,
            @RequestParam(required=false) String selfieData,
            @RequestParam String voterId,
            @RequestParam String name,
            @RequestParam String password,
            @RequestParam String confirm,
            @RequestParam(required=false) String otpVerified,
            Model model) {

        if (voterId == null || voterId.isBlank()) {
            model.addAttribute("error","Voter ID is required."); return "register";
        }
        if (password == null || password.length() < 6) {
            model.addAttribute("error","Password must be at least 6 characters."); return "register";
        }
        if (!password.equals(confirm)) {
            model.addAttribute("error","Passwords do not match."); return "register";
        }
        if (!"true".equals(otpVerified)) {
            model.addAttribute("error","Please verify your email with the OTP code."); return "register";
        }
        if (svc.voterIdExists(voterId)) {
            model.addAttribute("error","Voter ID '" + voterId + "' is already taken."); return "register";
        }

        // Face verification (optional)
        if (idPhoto != null && !idPhoto.isEmpty() && selfieData != null && !selfieData.isBlank()) {
            try {
                String base64 = selfieData.contains(",") ? selfieData.split(",")[1] : selfieData;
                byte[] selfieBytes = java.util.Base64.getDecoder().decode(base64);
                FaceVerificationService.FaceCompareResult result = faceService.compareFaces(idPhoto.getBytes(), selfieBytes);
                if (!result.passed) {
                    model.addAttribute("error","Face verification failed: " + result.message);
                    return "register";
                }
            } catch (Exception e) {
                System.err.println("Face verification error: " + e.getMessage());
            }
        }

        String fn = firstName != null && !firstName.isBlank() ? firstName : name;
        String ln = lastName  != null ? lastName : "";
        // Register with status=pending (awaiting admin approval)
        svc.addVoterFull(voterId, fn, middleName, ln, dateOfBirth, gender,
            street, barangay, city, province, zipCode, mobileNumber, email,
            voterIdNumber, votingDistrict, affiliation, idType, idNumber, password);

        // Save ID photo and selfie images for admin review
        try {
            byte[] idPhotoBytes = (idPhoto != null && !idPhoto.isEmpty()) ? idPhoto.getBytes() : null;
            byte[] selfieBytes  = null;
            if (selfieData != null && !selfieData.isBlank()) {
                String base64 = selfieData.contains(",") ? selfieData.split(",")[1] : selfieData;
                selfieBytes = java.util.Base64.getDecoder().decode(base64);
            }
            if (idPhotoBytes != null || selfieBytes != null) {
                svc.saveVoterImages(voterId, idPhotoBytes, selfieBytes);
            }
        } catch (Exception e) {
            System.err.println("Image save error: " + e.getMessage());
        }

        svc.logActivity(voterId, "Registered — awaiting admin approval");

        return "redirect:/login?success=registered";
    }

    // ── Forgot password ───────────────────────────────────────────────────────
    @GetMapping("/forgot-password")
    public String forgotPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String doForgot(@RequestParam String step,
                           @RequestParam(required=false) String voterId,
                           @RequestParam(required=false) String email,
                           @RequestParam(required=false) String otp,
                           @RequestParam(required=false) String newPassword,
                           @RequestParam(required=false) String confirm,
                           Model model) {
        switch (step) {
            case "request" -> {
                if (voterId == null || voterId.isBlank() || email == null || email.isBlank()) {
                    model.addAttribute("error","Please fill in all fields."); return "forgot-password";
                }
                Optional<Voter> voter = svc.findVoterByIdAndEmail(voterId, email);
                if (voter.isEmpty()) {
                    model.addAttribute("error","No account found with that Voter ID and email."); return "forgot-password";
                }
                String generatedOtp = emailService.generateOtp(voterId);
                String fallback = null;
                try { emailService.sendOtpEmail(email, voter.get().getName(), generatedOtp); }
                catch (Exception e) { fallback = generatedOtp; }
                String masked = email.replaceAll("(?<=.{2}).(?=.*@)","*");
                model.addAttribute("step","otp"); model.addAttribute("voterId",voterId);
                model.addAttribute("maskedEmail",masked);
                if (fallback != null) model.addAttribute("otpFallback",fallback);
            }
            case "verify-otp" -> {
                if (!emailService.verifyOtp(voterId, otp)) {
                    model.addAttribute("error","Invalid or expired code.");
                    model.addAttribute("step","otp"); model.addAttribute("voterId",voterId); return "forgot-password";
                }
                model.addAttribute("step","reset"); model.addAttribute("voterId",voterId);
            }
            case "reset" -> {
                if (newPassword == null || newPassword.length() < 6) {
                    model.addAttribute("error","Password must be at least 6 characters.");
                    model.addAttribute("step","reset"); model.addAttribute("voterId",voterId); return "forgot-password";
                }
                if (!newPassword.equals(confirm)) {
                    model.addAttribute("error","Passwords do not match.");
                    model.addAttribute("step","reset"); model.addAttribute("voterId",voterId); return "forgot-password";
                }
                svc.updatePassword(voterId, newPassword);
                return "redirect:/login?success=reset";
            }
        }
        return "forgot-password";
    }
}
