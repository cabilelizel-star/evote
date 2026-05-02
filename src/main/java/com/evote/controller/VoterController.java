package com.evote.controller;

import com.evote.service.ElectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/voter")
public class VoterController {

    private final ElectionService svc;
    public VoterController(ElectionService svc) { this.svc = svc; }

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
            return "redirect:/voter/dashboard?success=voted";
        }
        return "redirect:/voter/dashboard?error=" + result;
    }
}
