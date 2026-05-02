package com.evote.controller;

import com.evote.service.ElectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ElectionService svc;
    public AdminController(ElectionService svc) { this.svc = svc; }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";

        try {
            model.addAttribute("election",   svc.getElection());
            model.addAttribute("candidates", svc.getCandidates());
            model.addAttribute("voters",     svc.getVoters());
            model.addAttribute("totalVotes", svc.getTotalVotes());
        } catch (Exception e) {
            model.addAttribute("election",   new com.evote.model.Election(1, "General Election 2025", false));
            model.addAttribute("candidates", java.util.Collections.emptyList());
            model.addAttribute("voters",     java.util.Collections.emptyList());
            model.addAttribute("totalVotes", 0);
            model.addAttribute("dbError",    e.getMessage());
        }
        return "admin/dashboard";
    }

    // ── Candidate actions ─────────────────────────────────────────────────────
    @PostMapping("/candidate/add")
    public String addCandidate(@RequestParam String candidateId,
                               @RequestParam String name,
                               @RequestParam String party,
                               HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.addCandidate(candidateId.trim(), name.trim(), party.trim());
        return "redirect:/admin/dashboard?tab=candidates";
    }

    @PostMapping("/candidate/remove")
    public String removeCandidate(@RequestParam String candidateId,
                                  HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.removeCandidate(candidateId);
        return "redirect:/admin/dashboard?tab=candidates";
    }

    // ── Voter actions ─────────────────────────────────────────────────────────
    @PostMapping("/voter/add")
    public String addVoter(@RequestParam String voterId,
                           @RequestParam String name,
                           @RequestParam String password,
                           HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.addVoter(voterId.trim(), name.trim(), password);
        return "redirect:/admin/dashboard?tab=voters";
    }

    @PostMapping("/voter/remove")
    public String removeVoter(@RequestParam String voterId,
                              HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.removeVoter(voterId);
        return "redirect:/admin/dashboard?tab=voters";
    }

    // ── Election control ──────────────────────────────────────────────────────
    @PostMapping("/election/open")
    public String openElection(HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.setElectionOpen(true);
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/election/close")
    public String closeElection(HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.setElectionOpen(false);
        return "redirect:/admin/dashboard";
    }
}
