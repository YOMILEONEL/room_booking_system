package steve.bookingssystem.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UnauthenticatedAccessIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void withoutToken_returns401WithJsonBody() throws Exception {
        mockMvc.perform(get("/booking/getAll"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("{\"error\":\"Nicht authentifiziert\"}"));
    }

    @Test
    void withInvalidToken_returns401() throws Exception {
        mockMvc.perform(get("/booking/getAll").header("Authorization", "Bearer not.a.valid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Nicht authentifiziert"));
    }

    @Test
    void expiredTokenOfExistingUser_returns401() throws Exception {
        String email = "unauth-" + UUID.randomUUID() + "@test.example";
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "password123",
                                "customerType", "KUNDE",
                                "firstName", "Test",
                                "lastName", "User",
                                "phoneNumber", "0123456789"))))
                .andExpect(status().isOk());

        // Same secret/issuer as application-test.properties; signature and user are valid,
        // only "exp" lies in the past.
        SecretKey key = Keys.hmacShaKeyFor(
                "test-only-secret-key-not-for-production-use-0123456789".getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String expired = Jwts.builder()
                .issuer("bookingssystem-test")
                .subject(email)
                .issuedAt(Date.from(now.minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(now.minus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/booking/getAll").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("{\"error\":\"Nicht authentifiziert\"}"));
    }

    @Test
    void corsPreflightOnProtectedEndpoint_isNot401() throws Exception {
        mockMvc.perform(options("/booking/getAll")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void stripeWebhookWithoutSignatureHeader_returns400() throws Exception {
        mockMvc.perform(post("/payment/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerOnAdminEndpoint_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "unauth-" + UUID.randomUUID() + "@test.example",
                "password", "password123",
                "customerType", "KUNDE",
                "firstName", "Test",
                "lastName", "User",
                "phoneNumber", "0123456789"));
        String response = mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode auth = objectMapper.readTree(response);

        // /room/save is admin-only; a logged-in customer gets 403, not 401.
        mockMvc.perform(post("/room/save")
                        .header("Authorization", "Bearer " + auth.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Raum X", "capacity", 8, "location", "1. OG",
                                "city", "Musterstadt", "description", "Test", "pricePerDay", 100))))
                .andExpect(status().isForbidden());
    }
}
