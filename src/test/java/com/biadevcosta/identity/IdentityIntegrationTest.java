package com.biadevcosta.identity;

import com.biadevcosta.identity.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: real MySQL (Testcontainers), real Flyway migrations + admin seeder, real security
 * filter chain, over HTTP. Uses the committed test key pair ({@code application-test.yaml}).
 * Skipped when Docker is not available (see {@link AbstractIntegrationTest}).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IdentityIntegrationTest extends AbstractIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@hospital.local";
    private static final String ADMIN_PASSWORD = "admin12345";

    @Value("${local.server.port}")
    int port;

    private RestTestClient client;

    @BeforeEach
    void setUp() {
        client = RestTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    private static Consumer<HttpHeaders> json() {
        return headers -> headers.setContentType(MediaType.APPLICATION_JSON);
    }

    private static Consumer<HttpHeaders> jsonBearer(String accessToken) {
        return headers -> {
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(accessToken);
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> login(String email, String password) {
        return client.post().uri("/auth/login").headers(json())
                .body(Map.of("email", email, "password", password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .returnResult().getResponseBody();
    }

    @Test
    void seededAdmin_logsIn_andGetsATokenPair() {
        Map<String, Object> body = login(ADMIN_EMAIL, ADMIN_PASSWORD);

        assertThat(body).containsKeys("accessToken", "refreshToken");
        assertThat(body.get("tokenType")).isEqualTo("Bearer");
        assertThat(body.get("expiresIn")).isEqualTo(3600);
    }

    @Test
    void refresh_rotates_soTheOldRefreshTokenStopsWorking() {
        String refreshToken = (String) login(ADMIN_EMAIL, ADMIN_PASSWORD).get("refreshToken");

        client.post().uri("/auth/refresh").headers(json())
                .body(Map.of("refreshToken", refreshToken))
                .exchange().expectStatus().isOk();

        client.post().uri("/auth/refresh").headers(json())
                .body(Map.of("refreshToken", refreshToken))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void register_withoutToken_is401() {
        client.post().uri("/users").headers(json())
                .body(Map.of("email", "nobody@hospital.local", "password", "secret12345",
                        "role", "PATIENT", "fullName", "Nobody"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @SuppressWarnings("unchecked")
    void admin_registersADoctor_andTheProfileIsThenReadable() {
        String adminToken = (String) login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken");

        Map<String, Object> created = client.post().uri("/users").headers(jsonBearer(adminToken))
                .body(Map.of("email", "doctor@hospital.local", "password", "secret12345",
                        "role", "DOCTOR", "fullName", "Dr Strange", "crm", "CRM-42", "specialty", "Neuro"))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult().getResponseBody();

        String id = (String) created.get("id");

        client.get().uri("/users/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Dr Strange")
                .jsonPath("$.role").isEqualTo("DOCTOR");
    }

    @Test
    void nonAdminToken_cannotRegister_is403() {
        String adminToken = (String) login(ADMIN_EMAIL, ADMIN_PASSWORD).get("accessToken");
        client.post().uri("/users").headers(jsonBearer(adminToken))
                .body(Map.of("email", "nurse@hospital.local", "password", "secret12345",
                        "role", "NURSE", "fullName", "Nurse Joy"))
                .exchange().expectStatus().isCreated();

        String nurseToken = (String) login("nurse@hospital.local", "secret12345").get("accessToken");
        client.post().uri("/users").headers(jsonBearer(nurseToken))
                .body(Map.of("email", "someone@hospital.local", "password", "secret12345",
                        "role", "PATIENT", "fullName", "Someone"))
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void unknownUserId_is404() {
        client.get().uri("/users/does-not-exist").exchange().expectStatus().isNotFound();
    }
}
