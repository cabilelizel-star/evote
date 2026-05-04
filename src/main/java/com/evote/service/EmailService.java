package com.evote.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Sends emails via Gmail SMTP — lands in inbox, not spam.
 * OTPs are stored in the database so they survive app restarts.
 */
@Service
public class EmailService {

    @Value("${evote.mail.from:your_gmail@gmail.com}")
    private String fromEmail;

    private final JavaMailSender mailSender;
    private final JdbcTemplate   db;

    public EmailService(JavaMailSender mailSender, JdbcTemplate db) {
        this.mailSender = mailSender;
        this.db         = db;
    }

    // ── OTP — stored in DB ────────────────────────────────────────────────────
    public String generateOtp(String voterId) {
        String otp    = String.format("%06d", new Random().nextInt(1000000));
        long   expiry = System.currentTimeMillis() + 10 * 60 * 1000; // 10 min
        try {
            db.update("INSERT INTO otp_store (voter_id, otp_code, expires_at) VALUES (?,?,?) " +
                      "ON DUPLICATE KEY UPDATE otp_code=VALUES(otp_code), expires_at=VALUES(expires_at)",
                voterId, otp, expiry);
        } catch (Exception e) {
            System.err.println("OTP DB store failed: " + e.getMessage());
        }
        return otp;
    }

    public boolean verifyOtp(String voterId, String inputOtp) {
        try {
            var rows = db.queryForList(
                "SELECT otp_code, expires_at FROM otp_store WHERE voter_id = ?", voterId);
            if (rows.isEmpty()) return false;
            String stored = (String) rows.get(0).get("otp_code");
            long   expiry = ((Number) rows.get(0).get("expires_at")).longValue();
            if (System.currentTimeMillis() > expiry) {
                db.update("DELETE FROM otp_store WHERE voter_id = ?", voterId);
                return false;
            }
            boolean valid = stored.equals(inputOtp.trim());
            if (valid) db.update("DELETE FROM otp_store WHERE voter_id = ?", voterId);
            return valid;
        } catch (Exception e) {
            System.err.println("OTP verify error: " + e.getMessage());
            return false;
        }
    }

    // ── Send OTP ──────────────────────────────────────────────────────────────
    public void sendOtpEmail(String toEmail, String voterName, String otp) {
        String subject = "eVOTE — Your OTP Verification Code";
        String html = buildEmail("OTP Verification",
            "Hello, <strong>" + esc(voterName) + "</strong>!",
            "Your One-Time Password (OTP) for eVOTE registration is:",
            "<div style='font-size:42px;font-weight:900;letter-spacing:12px;color:#0044aa;" +
            "text-align:center;padding:24px;background:#e8f0fe;border-radius:12px;" +
            "border:2px dashed #0066cc;margin:20px 0'>" + otp + "</div>",
            "This OTP expires in <strong>10 minutes</strong>. Do not share it with anyone.",
            null, null);
        send(toEmail, subject, html);
    }

    // ── Vote confirmation ─────────────────────────────────────────────────────
    public void sendVoteConfirmation(String toEmail, String voterName,
                                      String ref, String timestamp, String electionTitle) {
        String subject = "eVOTE — Your Vote Has Been Cast ✅";
        String html = buildEmail("Vote Confirmed",
            "Thank you, <strong>" + esc(voterName) + "</strong>!",
            "Your vote has been successfully recorded in <strong>" + esc(electionTitle) + "</strong>.",
            "<div style='background:#e8f0fe;border-left:5px solid #0066cc;padding:16px 20px;" +
            "border-radius:8px;margin:20px 0'>" +
            "<div style='font-size:13px;color:#555;margin-bottom:4px'>Reference:</div>" +
            "<div style='font-size:16px;font-weight:800;color:#0044aa;font-family:monospace'>" + esc(ref) + "</div>" +
            "<div style='font-size:13px;color:#888;margin-top:6px'>" + esc(timestamp) + "</div></div>",
            "Your vote is <strong>final and secure</strong>. Thank you for participating.",
            "View Dashboard", "http://localhost:8080/voter/dashboard");
        send(toEmail, subject, html);
    }

    // ── Election opened ───────────────────────────────────────────────────────
    public void sendElectionOpenedEmail(String toEmail, String voterName, String electionTitle) {
        String subject = "🗳 " + electionTitle + " — Voting is Now OPEN!";
        String html = buildEmail("Election is Now Open!",
            "Hello, <strong>" + esc(voterName) + "</strong>!",
            "The election has officially started. You can now cast your vote.",
            "<div style='background:#e8f0fe;border-radius:12px;padding:24px;text-align:center;margin:20px 0'>" +
            "<div style='font-size:48px'>🗳</div>" +
            "<div style='font-size:20px;font-weight:800;color:#0044aa;margin:10px 0'>" + esc(electionTitle) + "</div>" +
            "<div style='font-size:14px;color:#555;font-weight:700'>✅ Voting is now ACTIVE</div></div>",
            "Log in now and cast your vote. Every vote counts!",
            "Vote Now →", "http://localhost:8080/login");
        send(toEmail, subject, html);
    }

    // ── Election closed ───────────────────────────────────────────────────────
    public void sendElectionClosedEmail(String toEmail, String voterName, String electionTitle) {
        String subject = "🔒 " + electionTitle + " — Voting Has Closed";
        String html = buildEmail("Election Closed",
            "Hello, <strong>" + esc(voterName) + "</strong>!",
            "Voting for <strong>" + esc(electionTitle) + "</strong> has officially ended.",
            "<div style='background:#fff8e1;border-left:5px solid #f59e0b;padding:16px 20px;" +
            "border-radius:8px;margin:20px 0;text-align:center'>" +
            "<div style='font-size:40px'>🔒</div>" +
            "<div style='font-size:16px;font-weight:700;color:#92400e;margin-top:8px'>Voting Period Has Ended</div>" +
            "</div>",
            "Thank you for your participation. Results will be announced shortly.",
            "View Dashboard", "http://localhost:8080/login");
        send(toEmail, subject, html);
    }

    // ── Announcement ─────────────────────────────────────────────────────────
    public void sendAnnouncement(String toEmail, String voterName, String subject, String message) {
        String html = buildEmail("Announcement",
            "Hello, <strong>" + esc(voterName) + "</strong>!",
            subject,
            "<div style='background:#f8faff;border-left:5px solid #0066cc;padding:16px 20px;" +
            "border-radius:8px;margin:20px 0;font-size:14px;color:#333;line-height:1.7'>" +
            esc(message).replace("\n", "<br>") + "</div>",
            "This is an official announcement from the eVOTE System administrator.",
            null, null);
        send(toEmail, subject, html);
    }

    // ── Core send ─────────────────────────────────────────────────────────────
    private void send(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail, "eVOTE Secured Ballot");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true); // true = HTML
            mailSender.send(msg);
            System.out.println("✅ Email sent to: " + to);
        } catch (Exception e) {
            System.err.println("❌ Email failed to " + to + ": " + e.getMessage());
        }
    }

    // ── HTML template ─────────────────────────────────────────────────────────
    private String buildEmail(String title, String greeting, String intro,
                               String highlight, String footer, String btnText, String btnUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>")
          .append("<body style='margin:0;padding:0;background:#f0f4ff;font-family:Segoe UI,Arial,sans-serif'>")
          .append("<table width='100%' cellpadding='0' cellspacing='0' style='background:#f0f4ff;padding:32px 0'>")
          .append("<tr><td align='center'>")
          .append("<table width='580' cellpadding='0' cellspacing='0' style='max-width:580px;width:100%'>")
          // Header
          .append("<tr><td style='background:linear-gradient(135deg,#0044aa,#0066cc);border-radius:16px 16px 0 0;padding:28px 32px;text-align:center'>")
          .append("<div style='font-size:32px;margin-bottom:6px'>🛡</div>")
          .append("<div style='color:#fff;font-size:22px;font-weight:900;letter-spacing:1px'>eVOTE SECURED BALLOT</div>")
          .append("<div style='color:rgba(255,255,255,0.75);font-size:12px;margin-top:4px'>Secure Electronic Voting Platform</div>")
          .append("</td></tr>")
          // Title bar
          .append("<tr><td style='background:#0033aa;padding:8px 32px;text-align:center'>")
          .append("<span style='color:#fff;font-size:13px;font-weight:700'>").append(esc(title)).append("</span>")
          .append("</td></tr>")
          // Body
          .append("<tr><td style='background:#fff;padding:32px 36px;border-radius:0 0 16px 16px'>")
          .append("<p style='font-size:16px;color:#222;margin:0 0 10px'>").append(greeting).append("</p>")
          .append("<p style='font-size:14px;color:#555;line-height:1.7;margin:0 0 8px'>").append(intro).append("</p>")
          .append(highlight)
          .append("<p style='font-size:13px;color:#777;line-height:1.7;margin:14px 0'>").append(footer).append("</p>");
        if (btnText != null && btnUrl != null) {
            sb.append("<div style='text-align:center;margin:24px 0'>")
              .append("<a href='").append(btnUrl).append("' style='display:inline-block;background:linear-gradient(135deg,#0066cc,#0044aa);color:#fff;text-decoration:none;padding:13px 32px;border-radius:8px;font-size:15px;font-weight:700'>")
              .append(esc(btnText)).append("</a></div>");
        }
        sb.append("<hr style='border:none;border-top:1px solid #e0eeff;margin:20px 0'>")
          .append("<p style='font-size:11px;color:#aaa;text-align:center;margin:0'>")
          .append("This is an automated message from eVOTE Secured Ballot. Do not reply.<br>")
          .append("© 2025 eVOTE System. All rights reserved.</p>")
          .append("</td></tr></table></td></tr></table></body></html>");
        return sb.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
                .replace("\"","&quot;").replace("'","&#39;");
    }
}
