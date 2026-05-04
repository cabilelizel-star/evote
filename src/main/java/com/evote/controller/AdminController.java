package com.evote.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.evote.service.ElectionService;
import com.evote.service.EmailService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ElectionService svc;
    private final EmailService    emailService;

    public AdminController(ElectionService svc, EmailService emailService) {
        this.svc          = svc;
        this.emailService = emailService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model, HttpServletResponse response) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        // Prevent browser AND proxy caching of dashboard
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader("Surrogate-Control", "no-store");
        try {
            java.util.List<com.evote.model.Candidate> all = svc.getCandidates();
            model.addAttribute("election",      svc.getElection());
            model.addAttribute("candidates",    all);
            model.addAttribute("voters",        svc.getVoters());
            model.addAttribute("pendingVoters", svc.getPendingVoters());
            model.addAttribute("totalVotes",    svc.getTotalVotes());
            model.addAttribute("turnoutPct",    svc.getTurnoutPercent());
            model.addAttribute("auditLogs",     svc.getAuditLogs());
            // Grouped by position for Live Results
            model.addAttribute("govCandidates",     filter(all, "Governor", false));
            model.addAttribute("viceGovCandidates", filter(all, "Vice Governor", false));
            model.addAttribute("mayorCandidates",   filter(all, "Mayor", false));
            model.addAttribute("viceMayorCandidates", filter(all, "Vice Mayor", false));
            model.addAttribute("repCandidates",     filter(all, "District Representative", false));
        } catch (Exception e) {
            model.addAttribute("election",      new com.evote.model.Election(1, "General Election 2025", false));
            model.addAttribute("candidates",    java.util.Collections.emptyList());
            model.addAttribute("voters",        java.util.Collections.emptyList());
            model.addAttribute("pendingVoters", java.util.Collections.emptyList());
            model.addAttribute("totalVotes",    0);
            model.addAttribute("turnoutPct",    0);
            model.addAttribute("auditLogs",     java.util.Collections.emptyList());
            model.addAttribute("govCandidates",     java.util.Collections.emptyList());
            model.addAttribute("viceGovCandidates", java.util.Collections.emptyList());
            model.addAttribute("mayorCandidates",   java.util.Collections.emptyList());
            model.addAttribute("viceMayorCandidates", java.util.Collections.emptyList());
            model.addAttribute("repCandidates",     java.util.Collections.emptyList());
            model.addAttribute("dbError",       e.getMessage());
        }
        return "admin/dashboard";
    }

    private java.util.List<com.evote.model.Candidate> filter(
            java.util.List<com.evote.model.Candidate> list, String keyword, boolean exact) {
        java.util.List<com.evote.model.Candidate> result = new java.util.ArrayList<>();
        for (com.evote.model.Candidate c : list) {
            String p = c.getParty() != null ? c.getParty() : "";
            if (p.contains(keyword)) result.add(c);
        }
        return result;
    }

    // ── Image serving ─────────────────────────────────────────────────────────
    @GetMapping("/voter/id-photo/{voterId}")
    @ResponseBody
    public ResponseEntity<byte[]> serveIdPhoto(@PathVariable String voterId, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role")))
            return ResponseEntity.status(403).build();
        byte[] img = svc.getVoterIdPhoto(voterId);
        if (img == null || img.length == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(img);
    }

    @GetMapping("/voter/selfie/{voterId}")
    @ResponseBody
    public ResponseEntity<byte[]> serveSelfie(@PathVariable String voterId, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role")))
            return ResponseEntity.status(403).build();
        byte[] img = svc.getVoterSelfie(voterId);
        if (img == null || img.length == 0) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(img);
    }

    // ── Approval actions ──────────────────────────────────────────────────────
    @PostMapping("/voter/approve")
    public String approveVoter(@RequestParam String voterId, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.approveVoter(voterId);
        svc.logActivity("Admin", "Approved voter: " + voterId);
        // Notify voter by email
        new Thread(() -> {
            try {
                svc.findVoterFull(voterId).ifPresent(v -> {
                    if (v.getEmail() != null && !v.getEmail().isBlank()) {
                        emailService.sendAnnouncement(v.getEmail(), v.getName(),
                            "✅ Registration Approved!",
                            "Your voter registration has been approved by the administrator. " +
                            "You can now log in and participate in the election.");
                    }
                });
            } catch (Exception ignored) {}
        }).start();
        return "redirect:/admin/dashboard?tab=approvals";
    }

    @PostMapping("/voter/reject")
    public String rejectVoter(@RequestParam String voterId,
                              @RequestParam(required=false) String reason,
                              HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        String r = (reason != null && !reason.isBlank()) ? reason : "Does not meet requirements.";
        svc.rejectVoter(voterId, r);
        svc.logActivity("Admin", "Rejected voter: " + voterId + " — " + r);
        new Thread(() -> {
            try {
                svc.findVoterFull(voterId).ifPresent(v -> {
                    if (v.getEmail() != null && !v.getEmail().isBlank()) {
                        emailService.sendAnnouncement(v.getEmail(), v.getName(),
                            "❌ Registration Rejected",
                            "Your voter registration was not approved. Reason: " + r +
                            "\n\nPlease contact the administrator for more information.");
                    }
                });
            } catch (Exception ignored) {}
        }).start();
        return "redirect:/admin/dashboard?tab=approvals";
    }

    // ── Candidate actions ─────────────────────────────────────────────────────
    @PostMapping("/candidate/add")
    public String addCandidate(@RequestParam String candidateId,
                               @RequestParam String name,
                               @RequestParam String party,
                               @RequestParam(required = false) String electionType,
                               HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.addCandidate(candidateId.trim(), name.trim(), party.trim(), electionType);
        svc.logActivity("Admin", "Added candidate: " + name + " (" + candidateId + ") [" + electionType + "]");
        return "redirect:/admin/dashboard?tab=candidates";
    }

    @PostMapping("/candidate/remove")
    public String removeCandidate(@RequestParam String candidateId, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.removeCandidate(candidateId);
        svc.logActivity("Admin", "Removed candidate: " + candidateId);
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
        svc.logActivity("Admin", "Registered voter: " + name + " (" + voterId + ")");
        return "redirect:/admin/dashboard?tab=voters";
    }

    @PostMapping("/voter/remove")
    public String removeVoter(@RequestParam String voterId, HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.removeVoter(voterId);
        svc.logActivity("Admin", "Removed voter: " + voterId);
        return "redirect:/admin/dashboard?tab=voters";
    }

    @PostMapping("/voter/block")
    public String blockVoter(@RequestParam String voterId,
                             @RequestParam(defaultValue="false") boolean blocked,
                             HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.setVoterBlocked(voterId, blocked);
        svc.logActivity("Admin", (blocked ? "Blocked" : "Unblocked") + " voter: " + voterId);
        return "redirect:/admin/dashboard?tab=voters";
    }

    // ── Election settings ─────────────────────────────────────────────────────
    @PostMapping("/election/update")
    public String updateElection(@RequestParam String title,
                                 @RequestParam(required = false) String startTime,
                                 @RequestParam(required = false) String endTime,
                                 @RequestParam(required = false) String electionType,
                                 @RequestParam(required = false) String organization,
                                 HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.updateElectionSettings(
            title != null && !title.isBlank() ? title.trim() : "General Election 2025",
            startTime, endTime, electionType, organization
        );
        svc.logActivity("Admin", "Updated election settings: " + title + " [" + electionType + "]");
        return "redirect:/admin/dashboard?tab=election&success=settingssaved";
    }

    // ── Election control ──────────────────────────────────────────────────────
    @PostMapping("/election/open")
    public String openElection(HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.setElectionOpen(true);
        String title = svc.getElection().getTitle();
        svc.logActivity("Admin", "Opened election: " + title);
        // Send in-app notification to all voters
        svc.sendNotificationToAll(
            "🗳 Election is Now OPEN!",
            title + " is now open. Log in and cast your vote now!"
        );
        // Send email to all voters
        new Thread(() -> svc.getVoters().stream()
            .filter(v -> v.getEmail() != null && !v.getEmail().isBlank())
            .forEach(v -> {
                try { emailService.sendElectionOpenedEmail(v.getEmail(), v.getName(), title); }
                catch (Exception ignored) {}
            })
        ).start();
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/election/close")
    public String closeElection(HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.setElectionOpen(false);
        String title = svc.getElection().getTitle();
        svc.logActivity("Admin", "Closed election: " + title);
        // Send in-app notification to all voters
        svc.sendNotificationToAll(
            "🔒 Election is Now CLOSED",
            title + " has ended. Thank you for participating!"
        );
        // Send email to all voters
        new Thread(() -> svc.getVoters().stream()
            .filter(v -> v.getEmail() != null && !v.getEmail().isBlank())
            .forEach(v -> {
                try { emailService.sendElectionClosedEmail(v.getEmail(), v.getName(), title); }
                catch (Exception ignored) {}
            })
        ).start();
        return "redirect:/admin/dashboard";
    }

    // ── Announcements ─────────────────────────────────────────────────────────
    @PostMapping("/announce")
    public String sendAnnouncement(@RequestParam String subject,
                                   @RequestParam String message,
                                   HttpSession session) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        svc.logActivity("Admin", "Sent announcement: " + subject);
        new Thread(() -> svc.getVoters().stream()
            .filter(v -> v.getEmail() != null && !v.getEmail().isBlank())
            .forEach(v -> emailService.sendAnnouncement(v.getEmail(), v.getName(), subject, message))
        ).start();
        return "redirect:/admin/dashboard?tab=announcements&success=sent";
    }

    // ── Admin password change ─────────────────────────────────────────────────
    @PostMapping("/change-password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session, Model model) {
        if (!"admin".equals(session.getAttribute("role"))) return "redirect:/login";
        if (!"Admin123".equals(currentPassword)) {
            return "redirect:/admin/dashboard?tab=profile&error=wrongpassword";
        }
        if (!newPassword.equals(confirmPassword) || newPassword.length() < 6) {
            return "redirect:/admin/dashboard?tab=profile&error=passwordmismatch";
        }
        // In a real system, store this securely. For now just log it.
        svc.logActivity("Admin", "Changed admin password");
        return "redirect:/admin/dashboard?tab=profile&success=passwordchanged";
    }
}
