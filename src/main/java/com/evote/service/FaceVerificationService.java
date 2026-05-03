package com.evote.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

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
    private static final double CONFIDENCE_THRESHOLD = 60.0; // lowered from 75% for better usability

    public FaceCompareResult compareFaces(byte[] idImageBytes, byte[] selfieBytes) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("Face++ API key not set — running in demo mode (verification skipped)");
            return new FaceCompareResult(true, 99.0, "Demo mode — face verification skipped");
        }

        String boundary = "----FormBoundary" + System.currentTimeMillis();
        HttpClient client = HttpClient.newHttpClient();

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

        if (json.contains("\"confidence\"")) {
            double confidence = parseDouble(json, "confidence");
            boolean passed = confidence >= CONFIDENCE_THRESHOLD;
            String msg = passed
                ? String.format("Face match confirmed (%.1f%% confidence)", confidence)
                : String.format("Face does not match ID (%.1f%% confidence). Please ensure good lighting and face the camera directly.", confidence);
            return new FaceCompareResult(passed, confidence, msg);
        }

        if (json.contains("FACE_NOT_FOUND")) {
            throw new Exception("No face detected. Please ensure your face is clearly visible and well-lit.");
        }
        if (json.contains("IMAGE_ERROR") || json.contains("INVALID_IMAGE")) {
            throw new Exception("Image quality too low. Please upload a clearer photo.");
        }
        if (json.contains("AUTHENTICATION_ERROR")) {
            throw new Exception("Face verification service configuration error. Please contact admin.");
        }

        // Log the full response for debugging
        System.err.println("Face++ unexpected response: " + json);
        throw new Exception("Face verification failed. Please try again or contact support.");
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
