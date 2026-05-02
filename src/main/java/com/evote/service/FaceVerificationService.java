package com.evote.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Uses Face++ Compare API to verify that a selfie matches an ID photo.
 * Free tier: 1000 calls/day, no credit card required.
 * Sign up at: https://console.faceplusplus.com/register
 */
@Service
public class FaceVerificationService {

    @Value("${facepp.api.key:}")
    private String apiKey;

    @Value("${facepp.api.secret:}")
    private String apiSecret;

    private static final String COMPARE_URL = "https://api-us.faceplusplus.com/facepp/v3/compare";
    private static final double CONFIDENCE_THRESHOLD = 75.0; // 75% match required

    /**
     * Compare two images — returns confidence score (0-100).
     * Throws exception if no face detected or API error.
     */
    public FaceCompareResult compareFaces(byte[] idImageBytes, byte[] selfieBytes) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            // Demo mode — skip real verification if no API key configured
            return new FaceCompareResult(true, 99.0, "Demo mode — face verification skipped");
        }

        String boundary = "----FormBoundary" + System.currentTimeMillis();
        HttpClient client = HttpClient.newHttpClient();

        // Build multipart body
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        writeField(body, boundary, "api_key",    apiKey);
        writeField(body, boundary, "api_secret", apiSecret);
        writeFile(body,  boundary, "image_file1", "id.jpg",     idImageBytes);
        writeFile(body,  boundary, "image_file2", "selfie.jpg", selfieBytes);
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(COMPARE_URL))
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String json = response.body();

        System.out.println("Face++ response: " + json);

        // Parse confidence from JSON manually (no extra dependency needed)
        if (json.contains("\"confidence\"")) {
            double confidence = parseDouble(json, "confidence");
            boolean passed = confidence >= CONFIDENCE_THRESHOLD;
            String msg = passed
                ? String.format("Face match confirmed (%.1f%% confidence)", confidence)
                : String.format("Face does not match ID (%.1f%% confidence, minimum %.0f%% required)",
                    confidence, CONFIDENCE_THRESHOLD);
            return new FaceCompareResult(passed, confidence, msg);
        }

        if (json.contains("FACE_NOT_FOUND")) {
            throw new Exception("No face detected in one of the images. Please retake the photo.");
        }
        if (json.contains("IMAGE_ERROR") || json.contains("INVALID_IMAGE")) {
            throw new Exception("Invalid image. Please upload a clear photo.");
        }

        throw new Exception("Face verification failed. Please try again.");
    }

    // ── Multipart helpers ─────────────────────────────────────────────────────
    private void writeField(ByteArrayOutputStream out, String boundary,
                             String name, String value) throws IOException {
        String part = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
            + value + "\r\n";
        out.write(part.getBytes(StandardCharsets.UTF_8));
    }

    private void writeFile(ByteArrayOutputStream out, String boundary,
                            String name, String filename, byte[] data) throws IOException {
        String header = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"" + name
            + "\"; filename=\"" + filename + "\"\r\n"
            + "Content-Type: image/jpeg\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private double parseDouble(String json, String key) {
        try {
            int idx = json.indexOf("\"" + key + "\"");
            if (idx < 0) return 0;
            int colon = json.indexOf(":", idx);
            int end   = json.indexOf(",", colon);
            if (end < 0) end = json.indexOf("}", colon);
            return Double.parseDouble(json.substring(colon + 1, end).trim());
        } catch (Exception e) { return 0; }
    }

    // ── Result DTO ────────────────────────────────────────────────────────────
    public static class FaceCompareResult {
        public final boolean passed;
        public final double  confidence;
        public final String  message;

        public FaceCompareResult(boolean passed, double confidence, String message) {
            this.passed     = passed;
            this.confidence = confidence;
            this.message    = message;
        }
    }
}
