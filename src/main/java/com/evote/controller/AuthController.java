package com.evote.controller;

import com.evote.model.Voter;
import com.evote.service.ElectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final ElectionService svc;
    public AuthController(ElectionService svc) { this.svc = svc; }

    // ── Login ─────────────────────────────────────────────────────────────────
    @GetMapping({"/", "/login"})
    public String loginPage(HttpSession session, Model model,
                            @RequestParam(required = false) String success) {
        if (session.getAttribute("userId") != null) return redirectByRole(session);
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

        // Admin
        if ("admin".equals(username) && "admin123".equals(password)) {
            session.setAttribute("userId",   "admin");
            session.setAttribute("userName", "Administrator");
            session.setAttribute("role",     "admin");
            return "redirect:/admin/dashboard";
        }

        // Voter
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

    // ── Register ──────────────────────────────────────────────────────────────
    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String doRegister(@RequestParam String voterId,
                             @RequestParam String name,
                             @RequestParam String password,
                             @RequestParam String confirm,
                             @RequestParam String question,
                             @RequestParam String answer,
                             Model model) {
        if (voterId.isBlank() || name.isBlank() || password.isBlank() || answer.isBlank()) {
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

        svc.addVoter(voterId, name, password);
        svc.saveSecurityQuestion(voterId, question, answer);
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
                model.addAttribute("step",     "answer");
                model.addAttribute("voterId",  voterId);
                model.addAttribute("question", q.get());
            }
            case "verify" -> {
                if (!svc.verifySecurityAnswer(voterId, answer)) {
                    model.addAttribute("error",    "Incorrect answer.");
                    model.addAttribute("step",     "answer");
                    model.addAttribute("voterId",  voterId);
                    svc.getSecurityQuestion(voterId).ifPresent(q -> model.addAttribute("question", q));
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

    private String redirectByRole(HttpSession session) {
        return "admin".equals(session.getAttribute("role"))
            ? "redirect:/admin/dashboard" : "redirect:/voter/dashboard";
    }
}
