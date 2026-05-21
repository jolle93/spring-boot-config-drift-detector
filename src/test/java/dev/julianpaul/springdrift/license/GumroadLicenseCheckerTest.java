package dev.julianpaul.springdrift.license;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GumroadLicenseCheckerTest {

    private HttpServer server;
    private GumroadLicenseChecker checker;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        int port = server.getAddress().getPort();
        checker = new GumroadLicenseChecker("http://localhost:" + port + "/v2/licenses/verify");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void verify_returnsSuccess_whenGumroadRespondsWithSuccessTrue() throws Exception {
        respondWith("{\"success\":true,\"message\":\"License is valid\"}");

        GumroadLicenseChecker.VerificationResult result = checker.verify("VALID-KEY");

        assertThat(result.success()).isTrue();
    }

    @Test
    void verify_returnsMessage_fromGumroadResponse() throws Exception {
        respondWith("{\"success\":false,\"message\":\"That license has already been redeemed.\"}");

        GumroadLicenseChecker.VerificationResult result = checker.verify("USED-KEY");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("That license has already been redeemed.");
    }

    @Test
    void verify_returnsFallbackMessage_whenNoMessageInResponse() throws Exception {
        respondWith("{\"success\":false}");

        GumroadLicenseChecker.VerificationResult result = checker.verify("BAD-KEY");

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Invalid license key.");
    }

    @Test
    void verify_sendsLicenseKeyInRequestBody() throws Exception {
        server.createContext("/v2/licenses/verify", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = body.contains("license_key=MY-LICENSE-KEY")
                    ? "{\"success\":true}".getBytes()
                    : "{\"success\":false,\"message\":\"Key not received\"}".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        GumroadLicenseChecker.VerificationResult result = checker.verify("MY-LICENSE-KEY");

        assertThat(result.success()).isTrue();
    }

    @Test
    void verify_sendsProductPermalinkInRequestBody() throws Exception {
        server.createContext("/v2/licenses/verify", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] response = body.contains("product_permalink=spring-drift")
                    ? "{\"success\":true}".getBytes()
                    : "{\"success\":false,\"message\":\"Permalink missing\"}".getBytes();
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        GumroadLicenseChecker.VerificationResult result = checker.verify("ANY-KEY");

        assertThat(result.success()).isTrue();
    }

    @Test
    void verify_throwsIOException_whenServerUnreachable() {
        server.stop(0);

        assertThatThrownBy(() -> checker.verify("ANY-KEY"))
                .isInstanceOf(ConnectException.class);
    }

    private void respondWith(String json) throws IOException {
        server.createContext("/v2/licenses/verify", exchange -> {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
    }
}
