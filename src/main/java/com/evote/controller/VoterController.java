package com.evote.controller;

import com.evote.model.Candidate;
import com.evote.model.Voter;
import com.evote.service.ElectionService;
import com.evote.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/voter")
public class VoterController {

    private final ElectionService svc;
    private final EmailService    emailService;

    public VoterController(ElectionService svc, EmailService emailService) {
        this.svc          = svc;
        this.emailService = emailService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!"voter".equals(session.getAttribute("role"))) return "redirect:/login";
        String voterId = (String) session.getAttribute("userId");
        try {
            com.evote.model.Election election = svc.getElection();
            Optional<Voter> voterOpt = svc.findVoterFull(voterId);
            Voter voter = voterOpt.orElse(null);

            model.addAttribute("election",   election);
            model.addAttribute("candidates", svc.getCandidates());
            model.addAttribute("voter",      voter);
            model.addAttribute("userName",   session.getAttribute("userName"));
            model.addAttribute("userId",     voterId);
            model.addAttribute("hasVoted",   session.getAttribute("hasVoted"));
            model.addAttribute("turnoutPct", svc.getTurnoutPercent());
        } catch (Exception e) {
            model.addAttribute("election",   new com.evote.model.Election(1, "General Election 2025", false));
            model.addAttribute("candidates", java.util.Collections.emptyList());
            model.addAttribute("userName",   session.getAttribute("userName"));
            model.addAttribute("userId",     voterId);
            model.addAttribute("hasVoted",   session.getAttribute("hasVoted"));
            model.addAttribute("dbError",    e.getMessage());
        }
        return "voter/dashboard";
    }

    @PostMapping("/vote")
    public String castVote(@RequestParam String candidateId, HttpSession session) {
        if (!"voter".equals(session.getAttribute("role"))) return "redirect:/login";
        String voterId = (String) session.getAttribute("userId");
        String result  = svc.castVote(voterId, candidateId);
        if ("ok".equals(result)) {
            session.setAttribute("hasVoted", true);
            Optional<Voter> voter = svc.findVoter(voterId);
            Optional<Candidate> candidate = svc.getCandidates().stream()
                .filter(c -> c.getCandidateId().equals(candidateId)).findFirst();
            if (voter.isPresent() && candidate.isPresent()
                    && voter.get().getEmail() != null && !voter.get().getEmail().isBlank()) {
                String em = voter.get().getEmail(), nm = voter.get().getName();
                String cn = candidate.get().getName(), pt = candidate.get().getParty();
                String et = svc.getElection().getTitle();
                new Thread(() -> emailService.sendVoteConfirmation(em, nm, cn, pt, et)).start();
            }
            return "redirect:/voter/dashboard?success=voted";
        }
        return "redirect:/voter/dashboard?error=" + result;
    }

    @GetMapping("/notifications")
    @ResponseBody
    public java.util.Map<String, Object> getNotifications(HttpSession session) {
        if (!"voter".equals(session.getAttribute("role")))
            return java.util.Map.of("error", "unauthorized");
        String voterId = (String) session.getAttribute("userId");
        return java.util.Map.of(
            "notifications", svc.getNotifications(voterId),
            "unread", svc.getUnreadCount(voterId)
        );
    }

    @PostMapping("/notifications/read")
    @ResponseBody
    public java.util.Map<String, String> markRead(HttpSession session) {
        if (!"voter".equals(session.getAttribute("role")))
            return java.util.Map.of("error", "unauthorized");
        svc.markAllRead((String) session.getAttribute("userId"));
        return java.util.Map.of("status", "ok");
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam(required=false) String contactNumber,
                                @RequestParam(required=false) String email,
                                HttpSession session) {
        if (!"voter".equals(session.getAttribute("role"))) return "redirect:/login";
        String voterId = (String) session.getAttribute("userId");
        svc.updateVoterContact(voterId, contactNumber, email);
        return "redirect:/voter/dashboard?tab=profile&success=updated";
    }

    @PostMapping("/profile/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session, Model model) {
        if (!"voter".equals(session.getAttribute("role"))) return "redirect:/login";
        String voterId = (String) session.getAttribute("userId");
        if (!svc.verifyVoterPassword(voterId, currentPassword)) {
            return "redirect:/voter/dashboard?tab=profile&error=wrongpassword";
        }
        if (!newPassword.equals(confirmPassword) || newPassword.length() < 6) {
            return "redirect:/voter/dashboard?tab=profile&error=passwordmismatch";
        }
        svc.updatePassword(voterId, newPassword);
        return "redirect:/voter/dashboard?tab=profile&success=passwordchanged";
    }
}
