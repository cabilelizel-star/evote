package com.evote.controller;

import com.evote.model.Candidate;
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

        try {
            model.addAttribute("election",   svc.getElection());
            model.addAttribute("candidates", svc.getCandidates());
            model.addAttribute("userName",   session.getAttribute("userName"));
            model.addAttribute("userId",     session.getAttribute("userId"));
            model.addAttribute("hasVoted",   session.getAttribute("hasVoted"));
        } catch (Exception e) {
            model.addAttribute("election",   new com.evote.model.Election(1, "General Election 2025", false));
            model.addAttribute("candidates", java.util.Collections.emptyList());
            model.addAttribute("userName",   session.getAttribute("userName"));
            model.addAttribute("userId",     session.getAttribute("userId"));
            model.addAttribute("hasVoted",   session.getAttribute("hasVoted"));
            model.addAttribute("dbError",    e.getMessage());
        }
        return "voter/dashboard";
    }

    @PostMapping("/vote")
    public String castVote(@RequestParam String candidateId,
                           HttpSession session) {
        if (!"voter".equals(session.getAttribute("role"))) return "redirect:/login";

        String voterId = (String) session.getAttribute("userId");
        String result  = svc.castVote(voterId, candidateId);

        if ("ok".equals(result)) {
            session.setAttribute("hasVoted", true);
            // Send vote confirmation email in background
            Optional<com.evote.model.Voter> voter = svc.findVoter(voterId);
            Optional<Candidate> candidate = svc.getCandidates().stream()
                .filter(c -> c.getCandidateId().equals(candidateId)).findFirst();
            if (voter.isPresent() && candidate.isPresent()
                    && voter.get().getEmail() != null && !voter.get().getEmail().isBlank()) {
                String email     = voter.get().getEmail();
                String name      = voter.get().getName();
                String cName     = candidate.get().getName();
                String party     = candidate.get().getParty();
                String elTitle   = svc.getElection().getTitle();
                new Thread(() -> emailService.sendVoteConfirmation(email, name, cName, party, elTitle)).start();
            }
            return "redirect:/voter/dashboard?success=voted";
        }
        return "redirect:/voter/dashboard?error=" + result;
    }
}
