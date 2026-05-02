package com.evote.controller;

import com.evote.model.Candidate;
import com.evote.model.Election;
import com.evote.service.ElectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final ElectionService svc;
    public ApiController(ElectionService svc) { this.svc = svc; }

    /** Real-time stats for admin dashboard */
    @GetMapping("/stats")
    public Map<String, Object> stats(HttpSession session) {
        if (!"admin".equals(session.getAttribute("role")))
            return Map.of("error", "unauthorized");

        Election e = svc.getElection();
        List<Candidate> candidates = svc.getCandidates();
        int totalVotes = svc.getTotalVotes();
        int totalVoters = svc.getVoters().size();

        List<Map<String, Object>> cList = new ArrayList<>();
        for (Candidate c : candidates) {
            double pct = totalVotes > 0 ? (c.getVoteCount() * 100.0 / totalVotes) : 0;
            cList.add(Map.of(
                "id",        c.getCandidateId(),
                "name",      c.getName(),
                "party",     c.getParty(),
                "votes",     c.getVoteCount(),
                "pct",       String.format("%.1f", pct)
            ));
        }

        return Map.of(
            "electionOpen",  e.isOpen(),
            "totalVotes",    totalVotes,
            "totalVoters",   totalVoters,
            "totalCandidates", candidates.size(),
            "candidates",    cList,
            "timestamp",     System.currentTimeMillis()
        );
    }

    /** Real-time election status for voter dashboard */
    @GetMapping("/election-status")
    public Map<String, Object> electionStatus(HttpSession session) {
        if (!"voter".equals(session.getAttribute("role")))
            return Map.of("error", "unauthorized");

        Election e = svc.getElection();
        return Map.of(
            "open",      e.isOpen(),
            "title",     e.getTitle(),
            "timestamp", System.currentTimeMillis()
        );
    }
}
