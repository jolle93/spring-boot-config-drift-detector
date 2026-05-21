package dev.julianpaul.springdrift.license;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GumroadLicenseChecker {

    private static final String PRODUCT_PERMALINK = "spring-drift";
    private static final String DEFAULT_VERIFY_URL = "https://api.gumroad.com/v2/licenses/verify";

    private final String verifyUrl;

    public GumroadLicenseChecker() {
        this(DEFAULT_VERIFY_URL);
    }

    GumroadLicenseChecker(String verifyUrl) {
        this.verifyUrl = verifyUrl;
    }

    public record VerificationResult(boolean success, String message) {}

    public VerificationResult verify(String licenseKey) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String body = "product_permalink=" + URLEncoder.encode(PRODUCT_PERMALINK, StandardCharsets.UTF_8)
                + "&license_key=" + URLEncoder.encode(licenseKey, StandardCharsets.UTF_8)
                + "&increment_uses_count=false";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(verifyUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResponse(response.body());
    }

    private VerificationResult parseResponse(String json) {
        boolean success = json.contains("\"success\":true");
        String message = extractJsonString(json, "message");
        if (message == null) {
            message = success ? "License is valid." : "Invalid license key.";
        }
        return new VerificationResult(success, message);
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }
}
