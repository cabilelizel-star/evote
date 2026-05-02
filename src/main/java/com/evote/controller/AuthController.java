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
            session.setAttribute("userName", "Administrator");
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
    public String sendOtp(@RequestParam String voterId,
                          @RequestParam String name,
                          @RequestParam String email,
                          @RequestParam String password,
                          @RequestParam String confirm,
                          HttpSession session, Model model) {
        if (voterId.isBlank() || name.isBlank() || email.isBlank() || password.isBlank()) {
            model.addAttribute("error", "All fields are required."); return "register";
        }
        if (password.length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters."); return "register";
        }
        if (!password.equals(confirm)) {
            model.addAttribute("error", "Passwords do not match."); return "register";
        }
        if (svc.voterIdExists(voterId)) {
            model.addAttribute("error", "Voter ID '" + voterId + "' is already taken."); return "register";
        }

        // Generate and send OTP
        String otp = emailService.generateOtp(voterId);
        try {
            emailService.sendOtpEmail(email, name, otp);
        } catch (Exception e) {
            System.err.println("OTP email failed: " + e.getMessage());
            // Continue anyway — show OTP in session for demo if email fails
        }

        // Store form data in session for final submission
        session.setAttribute("reg_voterId",  voterId);
        session.setAttribute("reg_name",     name);
        session.setAttribute("reg_email",    email);
        session.setAttribute("reg_password", password);

        return "redirect:/register/verify-otp";
    }

    // ── Register — Step 3: verify OTP ────────────────────────────────────────
    @GetMapping("/register/verify-otp")
    public String otpPage(HttpSession session, Model model) {
        if (session.getAttribute("reg_voterId") == null) return "redirect:/register";
        model.addAttribute("email", session.getAttribute("reg_email"));
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
    public String doComplete(@RequestParam(required = false) String birthday,
                             @RequestParam(required = false) Integer age,
                             @RequestParam(required = false) String placeOfBirth,
                             @RequestParam(required = false) String gender,
                             @RequestParam(required = false) String contactNumber,
                             @RequestParam(required = false) String idType,
                             @RequestParam(required = false) MultipartFile idPhoto,
                             @RequestParam(required = false) String selfieData,
                             HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute("otp_verified"))) return "redirect:/register";

        String voterId  = (String) session.getAttribute("reg_voterId");
        String name     = (String) session.getAttribute("reg_name");
        String email    = (String) session.getAttribute("reg_email");
        String password = (String) session.getAttribute("reg_password");

        // Face verification
        if (idPhoto != null && !idPhoto.isEmpty() && selfieData != null && !selfieData.isBlank()) {
            try {
                String base64    = selfieData.contains(",") ? selfieData.split(",")[1] : selfieData;
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

        svc.addVoterFull(voterId, name, email, birthday, age, placeOfBirth, gender, contactNumber, password);

        // Clear session
        session.removeAttribute("reg_voterId");
        session.removeAttribute("reg_name");
        session.removeAttribute("reg_email");
        session.removeAttribute("reg_password");
        session.removeAttribute("otp_verified");

        return "redirect:/login?success=registered";
    }

    // ── Forgot password ───────────────────────────────────────────────────────
    @GetMapping("/forgot-password")
    public String forgotPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String doForgot(@RequestParam String step,
                           @RequestParam(required = false) String voterId,
                           @RequestParam(required = false) String answer,
                           @RequestParam(required = false) String newPassword,
                           @RequestParam(required = false) String confirm,
                           Model model) {
        switch (step) {
            case "lookup" -> {
                Optional<String> q = svc.getSecurityQuestion(voterId);
                if (q.isEmpty()) {
                    model.addAttribute("error", "No account found with that Voter ID.");
                    return "forgot-password";
                }
                model.addAttribute("step", "answer");
                model.addAttribute("voterId", voterId);
                model.addAttribute("question", q.get());
            }
            case "verify" -> {
                if (!svc.verifySecurityAnswer(voterId, answer)) {
                    model.addAttribute("error", "Incorrect answer.");
                    model.addAttribute("step", "answer");
                    model.addAttribute("voterId", voterId);
                    svc.getSecurityQuestion(voterId).ifPresent(q -> model.addAttribute("question", q));
                    return "forgot-password";
                }
                model.addAttribute("step", "reset");
                model.addAttribute("voterId", voterId);
            }
            case "reset" -> {
                if (newPassword == null || newPassword.length() < 6) {
                    model.addAttribute("error", "Password must be at least 6 characters.");
                    model.addAttribute("step", "reset");
                    model.addAttribute("voterId", voterId);
                    return "forgot-password";
                }
                if (!newPassword.equals(confirm)) {
                    model.addAttribute("error", "Passwords do not match.");
                    model.addAttribute("step", "reset");
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
