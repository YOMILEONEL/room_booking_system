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

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
