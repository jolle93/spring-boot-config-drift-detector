package dev.julianpaul.springdrift.license;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GumroadLicenseChecker {

    private static final String PRODUCT_PERMALINK = "spring-drift";
    private static final String DEFAULT_VERIFY_URL = "https://api.gumroad.com/v2/licenses/verify";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final Pattern SUCCESS_PATTERN = Pattern.compile("\"success\"\\s*:\\s*true");

    private final String verifyUrl;

    public GumroadLicenseChecker() {
        this(DEFAULT_VERIFY_URL);
    }

    GumroadLicenseChecker(String verifyUrl) {
        this.verifyUrl = verifyUrl;
    }

    public record VerificationResult(boolean success, String message) {}

    public VerificationResult verify(String licenseKey) throws IOException, InterruptedException {
        String body = "product_permalink=" + URLEncoder.encode(PRODUCT_PERMALINK, StandardCharsets.UTF_8)
                + "&license_key=" + URLEncoder.encode(licenseKey, StandardCharsets.UTF_8)
                + "&increment_uses_count=false";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(verifyUrl))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            return new VerificationResult(false, "Server returned status code: " + response.statusCode());
        }
        return parseResponse(response.body());
    }

    private VerificationResult parseResponse(String json) {
        boolean success = SUCCESS_PATTERN.matcher(json).find();
        String message = extractJsonString(json, "message");
        if (message == null) {
            message = success ? "License is valid." : "Invalid license key.";
        }
        return new VerificationResult(success, message);
    }

    private String extractJsonString(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }
}
