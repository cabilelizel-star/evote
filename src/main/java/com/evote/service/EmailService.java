package com.evote.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends emails via Resend HTTP API (https://resend.com) — works on Railway.
 * Free tier: 3,000 emails/month, 100/day.
 * Set RESEND_API_KEY in Railway Variables.
 *
 * Fallback: if no API key, OTP is shown on screen.
 */
@Service
public class EmailService {

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${resend.from.email:onboarding@resend.dev}")
    private String fromEmail;

    private static final String RESEND_URL = "https://api.resend.com/emails";
    private final Map<String, long[]> otpStore = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newHttpClient();

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

    // ── Send OTP ──────────────────────────────────────────────────────────────
    public void sendOtpEmail(String toEmail, String voterName, String otp) {
        String subject = "E-Vote System — Your OTP Verification Code";
        String html = buildEmail("OTP Verification",
            "Hello, <strong>" + esc(voterName) + "</strong>!",
            "Your One-Time Password (OTP) for E-Vote System registration is:",
            "<div style='font-size:42px;font-weight:900;letter-spacing:12px;color:#1a6b3c;" +
            "text-align:center;padding:24px;background:#f0faf4;border-radius:12px;" +
            "border:2px dashed #2d9e5f;margin:20px 0'>" + otp + "</div>",
            "This OTP expires in <strong>10 minutes</strong>. Do not share it with anyone.",
            null, null);
        send(toEmail, subject, html);
    }

    // ── Vote confirmation ─────────────────────────────────────────────────────
    public void sendVoteConfirmation(String toEmail, String voterName,
                                      String candidateName, String party, String electionTitle) {
        String subject = "E-Vote System — Your Vote Has Been Cast ✅";
        String html = buildEmail("Vote Confirmed",
            "Thank you, <strong>" + esc(voterName) + "</strong>!",
            "Your vote has been successfully recorded in <strong>" + esc(electionTitle) + "</strong>.",
            "<div style='background:#f0faf4;border-left:5px solid #2d9e5f;padding:16px 20px;" +
            "border-radius:8px;margin:20px 0'>" +
            "<div style='font-size:13px;color:#888;margin-bottom:4px'>You voted for:</div>" +
            "<div style='font-size:20px;font-weight:800;color:#1a6b3c'>" + esc(candidateName) + "</div>" +
            "<div style='font-size:14px;color:#555'>" + esc(party) + "</div></div>",
            "Your vote is <strong>final and secure</strong>. Thank you for participating.",
            "🗳 View Dashboard", "https://web-production-fa2c8.up.railway.app/voter/dashboard");
        send(toEmail, subject, html);
    }

    // ── Election opened ───────────────────────────────────────────────────────
    public void sendElectionOpenedEmail(String toEmail, String voterName, String electionTitle) {
        String subject = "🗳 " + electionTitle + " — Voting is Now OPEN!";
        String html = buildEmail("Election is Open!",
            "Hello, <strong>" + esc(voterName) + "</strong>!",
            "The election has officially started. You can now cast your vote.",
            "<div style='background:#f0faf4;border-radius:12px;padding:20px;text-align:center;margin:20px 0'>" +
            "<div style='font-size:36px'>🗳</div>" +
            "<div style='font-size:18px;font-weight:800;color:#1a6b3c;margin:8px 0'>" + esc(electionTitle) + "</div>" +
            "<div style='font-size:13px;color:#555'>Voting is now active</div></div>",
            "Log in now to cast your vote. Every vote counts!",
            "Vote Now →", "https://web-production-fa2c8.up.railway.app/login");
        send(toEmail, subject, html);
    }

    // ── Election closed ───────────────────────────────────────────────────────
    public void sendElectionClosedEmail(String toEmail, String voterName, String electionTitle) {
        String subject = "🔒 " + electionTitle + " — Voting Has Closed";
        String html = buildEmail("Election Closed",
            "Hello, <strong>" + esc(voterName) + "</strong>!",
            "Voting for <strong>" + esc(electionTitle) + "</strong> has officially ended.",
            "<div style='background:#fff8f0;border-left:5px solid #f39c12;padding:16px 20px;" +
            "border-radius:8px;margin:20px 0;text-align:center'>" +
            "<div style='font-size:32px'>🔒</div>" +
            "<div style='font-size:16px;font-weight:700;color:#d68910;margin-top:8px'>Voting Period Has Ended</div>" +
            "</div>",
            "Thank you for your participation. Results will be announced shortly.",
            "View Results →", "https://web-production-fa2c8.up.railway.app/login");
        send(toEmail, subject, html);
    }

    // ── Announcement ─────────────────────────────────────────────────────────
    public void sendAnnouncement(String toEmail, String voterName, String subject, String message) {
        String html = buildEmail("Announcement",
            "Hello, <strong>" + esc(voterName) + "</strong>!",
            subject,
            "<div style='background:#f0faf4;border-left:5px solid #2d9e5f;padding:16px 20px;" +
            "border-radius:8px;margin:20px 0;font-size:14px;color:#333;line-height:1.7'>" +
            esc(message).replace("\n", "<br>") + "</div>",
            "This is an official announcement from the E-Vote System administrator.",
            null, null);
        send(toEmail, subject, html);
    }
    private void send(String to, String subject, String html) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            System.err.println("❌ RESEND_API_KEY not set — email not sent to " + to);
            throw new RuntimeException("Email service not configured. Set RESEND_API_KEY in Railway Variables.");
        }

        // Build JSON payload
        String json = "{"
            + "\"from\":\"" + esc(fromEmail) + "\","
            + "\"to\":[\"" + esc(to) + "\"],"
            + "\"subject\":\"" + esc(subject) + "\","
            + "\"html\":\"" + escJson(html) + "\""
            + "}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RESEND_URL))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();

            if (status == 200 || status == 201) {
                System.out.println("✅ Email sent to: " + to + " (status " + status + ")");
            } else {
                System.err.println("❌ Resend API error " + status + ": " + response.body());
                throw new RuntimeException("Email API returned status " + status + ": " + response.body());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Email send failed: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // ── Email HTML template ───────────────────────────────────────────────────
    private String buildEmail(String title, String greeting, String intro,
                               String highlight, String footer, String btnText, String btnUrl) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head>")
          .append("<body style='margin:0;padding:0;background:#f4faf6;font-family:Segoe UI,Arial,sans-serif'>")
          .append("<table width='100%' cellpadding='0' cellspacing='0' style='background:#f4faf6;padding:40px 0'>")
          .append("<tr><td align='center'>")
          .append("<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>")
          // Header
          .append("<tr><td style='background:linear-gradient(135deg,#1a6b3c,#2d9e5f);border-radius:16px 16px 0 0;padding:32px;text-align:center'>")
          .append("<div style='font-size:48px;margin-bottom:8px'>🗳</div>")
          .append("<div style='color:#fff;font-size:24px;font-weight:900;letter-spacing:1px'>E-VOTE SYSTEM</div>")
          .append("<div style='color:rgba(255,255,255,0.8);font-size:13px;margin-top:4px'>Secure Electronic Voting Platform</div>")
          .append("</td></tr>")
          // Title bar
          .append("<tr><td style='background:#1a6b3c;padding:10px 32px;text-align:center'>")
          .append("<span style='color:#fff;font-size:14px;font-weight:700'>").append(esc(title)).append("</span>")
          .append("</td></tr>")
          // Body
          .append("<tr><td style='background:#fff;padding:36px 40px;border-radius:0 0 16px 16px'>")
          .append("<p style='font-size:16px;color:#333;margin:0 0 12px'>").append(greeting).append("</p>")
          .append("<p style='font-size:14px;color:#555;line-height:1.7;margin:0 0 8px'>").append(intro).append("</p>")
          .append(highlight)
          .append("<p style='font-size:13px;color:#777;line-height:1.7;margin:16px 0'>").append(footer).append("</p>");
        if (btnText != null && btnUrl != null) {
            sb.append("<div style='text-align:center;margin:28px 0'>")
              .append("<a href='").append(btnUrl).append("' style='display:inline-block;background:linear-gradient(135deg,#2d9e5f,#1a6b3c);color:#fff;text-decoration:none;padding:14px 36px;border-radius:8px;font-size:15px;font-weight:700'>")
              .append(esc(btnText)).append("</a></div>");
        }
        sb.append("<hr style='border:none;border-top:1px solid #e0f2e9;margin:24px 0'>")
          .append("<p style='font-size:11px;color:#aaa;text-align:center;margin:0'>")
          .append("This is an automated message from E-Vote System. Do not reply.<br>")
          .append("© 2025 E-Vote System. All rights reserved.</p>")
          .append("</td></tr></table></td></tr></table></body></html>");
        return sb.toString();
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}
