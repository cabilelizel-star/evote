package com.evote.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    // OTP store: voterId -> {otp, expiry}
    private final Map<String, long[]> otpStore = new ConcurrentHashMap<>();

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ── OTP ───────────────────────────────────────────────────────────────────
    public String generateOtp(String voterId) {
        String otp = String.format("%06d", new Random().nextInt(1000000));
        long expiry = System.currentTimeMillis() + 10 * 60 * 1000; // 10 min
        otpStore.put(voterId, new long[]{Long.parseLong(otp), expiry});
        return otp;
    }

    public boolean verifyOtp(String voterId, String inputOtp) {
        long[] data = otpStore.get(voterId);
        if (data == null) return false;
        if (System.currentTimeMillis() > data[1]) { otpStore.remove(voterId); return false; }
        boolean valid = String.valueOf((long) data[0]).equals(inputOtp.trim());
        if (valid) otpStore.remove(voterId);
        return valid;
    }

    // ── Send OTP email ────────────────────────────────────────────────────────
    public void sendOtpEmail(String toEmail, String voterName, String otp) {
        String subject = "E-Vote System — Your OTP Verification Code";
        String html = buildEmail(
            "OTP Verification",
            "Hello, <strong>" + voterName + "</strong>!",
            "Your One-Time Password (OTP) for E-Vote System registration is:",
            "<div style='font-size:42px;font-weight:900;letter-spacing:12px;color:#1a6b3c;" +
            "text-align:center;padding:24px;background:#f0faf4;border-radius:12px;" +
            "border:2px dashed #2d9e5f;margin:20px 0'>" + otp + "</div>",
            "This OTP expires in <strong>10 minutes</strong>. Do not share it with anyone.",
            null, null
        );
        send(toEmail, subject, html);
    }

    // ── Vote confirmation ─────────────────────────────────────────────────────
    public void sendVoteConfirmation(String toEmail, String voterName,
                                      String candidateName, String party,
                                      String electionTitle) {
        String subject = "E-Vote System — Your Vote Has Been Cast ✅";
        String html = buildEmail(
            "Vote Confirmed",
            "Thank you, <strong>" + voterName + "</strong>!",
            "Your vote has been successfully recorded in <strong>" + electionTitle + "</strong>.",
            "<div style='background:#f0faf4;border-left:5px solid #2d9e5f;padding:16px 20px;" +
            "border-radius:8px;margin:20px 0'>" +
            "<div style='font-size:13px;color:#888;margin-bottom:4px'>You voted for:</div>" +
            "<div style='font-size:20px;font-weight:800;color:#1a6b3c'>" + candidateName + "</div>" +
            "<div style='font-size:14px;color:#555'>" + party + "</div></div>",
            "Your vote is <strong>final and secure</strong>. Thank you for participating in the democratic process.",
            "🗳 View Election Results",
            "https://web-production-fa2c8.up.railway.app/voter/dashboard"
        );
        send(toEmail, subject, html);
    }

    // ── Election opened notification ──────────────────────────────────────────
    public void sendElectionOpenedEmail(String toEmail, String voterName, String electionTitle) {
        String subject = "🗳 " + electionTitle + " — Voting is Now OPEN!";
        String html = buildEmail(
            "Election is Open!",
            "Hello, <strong>" + voterName + "</strong>!",
            "The election has officially started. You can now cast your vote.",
            "<div style='background:#f0faf4;border-radius:12px;padding:20px;text-align:center;margin:20px 0'>" +
            "<div style='font-size:36px'>🗳</div>" +
            "<div style='font-size:18px;font-weight:800;color:#1a6b3c;margin:8px 0'>" + electionTitle + "</div>" +
            "<div style='font-size:13px;color:#555'>Voting is now active</div></div>",
            "Log in now to cast your vote. Every vote counts!",
            "Vote Now →",
            "https://web-production-fa2c8.up.railway.app/login"
        );
        send(toEmail, subject, html);
    }

    // ── Election closed notification ──────────────────────────────────────────
    public void sendElectionClosedEmail(String toEmail, String voterName, String electionTitle) {
        String subject = "🔒 " + electionTitle + " — Voting Has Closed";
        String html = buildEmail(
            "Election Closed",
            "Hello, <strong>" + voterName + "</strong>!",
            "Voting for <strong>" + electionTitle + "</strong> has officially ended.",
            "<div style='background:#fff8f0;border-left:5px solid #f39c12;padding:16px 20px;" +
            "border-radius:8px;margin:20px 0;text-align:center'>" +
            "<div style='font-size:32px'>🔒</div>" +
            "<div style='font-size:16px;font-weight:700;color:#d68910;margin-top:8px'>Voting Period Has Ended</div>" +
            "</div>",
            "Thank you for your participation. Results will be announced shortly.",
            "View Results →",
            "https://web-production-fa2c8.up.railway.app/login"
        );
        send(toEmail, subject, html);
    }

    // ── Email template ────────────────────────────────────────────────────────
    private String buildEmail(String title, String greeting, String intro,
                               String highlight, String footer,
                               String btnText, String btnUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body style='margin:0;padding:0;background:#f4faf6;font-family:Segoe UI,Arial,sans-serif'>");

        // Wrapper
        sb.append("<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4faf6;padding:40px 0'><tr><td align='center'>");
        sb.append("<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>");

        // Header
        sb.append("<tr><td style='background:linear-gradient(135deg,#1a6b3c,#2d9e5f);border-radius:16px 16px 0 0;padding:32px;text-align:center'>");
        sb.append("<table cellpadding='0' cellspacing='0' align='center'><tr><td>");
        // Logo
        sb.append("<div style='display:inline-block;background:rgba(255,255,255,0.15);border-radius:50%;width:72px;height:72px;line-height:72px;text-align:center;font-size:36px;margin-bottom:12px'>🗳</div>");
        sb.append("<div style='color:#fff;font-size:26px;font-weight:900;letter-spacing:1px'>E-VOTE SYSTEM</div>");
        sb.append("<div style='color:rgba(255,255,255,0.8);font-size:13px;margin-top:4px'>Secure Electronic Voting Platform</div>");
        sb.append("</td></tr></table></td></tr>");

        // Title bar
        sb.append("<tr><td style='background:#1a6b3c;padding:12px 32px;text-align:center'>");
        sb.append("<span style='color:#fff;font-size:15px;font-weight:700;letter-spacing:.5px'>" + title + "</span>");
        sb.append("</td></tr>");

        // Body
        sb.append("<tr><td style='background:#fff;padding:36px 40px;border-radius:0 0 16px 16px'>");
        sb.append("<p style='font-size:16px;color:#333;margin:0 0 12px'>" + greeting + "</p>");
        sb.append("<p style='font-size:14px;color:#555;line-height:1.7;margin:0 0 8px'>" + intro + "</p>");
        sb.append(highlight);
        sb.append("<p style='font-size:13px;color:#777;line-height:1.7;margin:16px 0'>" + footer + "</p>");

        // Button
        if (btnText != null && btnUrl != null) {
            sb.append("<div style='text-align:center;margin:28px 0'>");
            sb.append("<a href='" + btnUrl + "' style='display:inline-block;background:linear-gradient(135deg,#2d9e5f,#1a6b3c);color:#fff;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:15px;font-weight:700;letter-spacing:.3px'>" + btnText + "</a>");
            sb.append("</div>");
        }

        // Divider
        sb.append("<hr style='border:none;border-top:1px solid #e0f2e9;margin:24px 0'>");

        // Footer
        sb.append("<p style='font-size:11px;color:#aaa;text-align:center;margin:0'>");
        sb.append("This is an automated message from E-Vote System. Please do not reply to this email.<br>");
        sb.append("© 2025 E-Vote System. All rights reserved.</p>");
        sb.append("</td></tr>");

        // Bottom bar
        sb.append("<tr><td style='padding:16px;text-align:center'>");
        sb.append("<p style='font-size:11px;color:#aaa;margin:0'>Secure • Transparent • Trusted</p>");
        sb.append("</td></tr>");

        sb.append("</table></td></tr></table></body></html>");
        return sb.toString();
    }

    // ── Send helper ───────────────────────────────────────────────────────────
    private void send(String to, String subject, String html) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail, "E-Vote System");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            System.out.println("Email sent to: " + to);
        } catch (Exception e) {
            System.err.println("Email send failed: " + e.getMessage());
        }
    }
}
