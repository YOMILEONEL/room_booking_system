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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailCollisionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode register(String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "password123",
                "customerType", "KUNDE",
                "firstName", "Test",
                "lastName", "User",
                "phoneNumber", "0123456789"
        ));
        String response = mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    // Regression test for docs/code-review.md 1.9: a member used to be able to rename themselves
    // to another user's email via PUT /user/update/{ownId}, after which findByEmail returned
    // two rows for that email and broke every request (login, auth check, ...) for the victim.
    @Test
    void renamingToAnExistingEmail_isRejectedAndVictimStaysUsable() throws Exception {
        String suffix = UUID.randomUUID().toString();
        JsonNode victim = register("victim-" + suffix + "@test.example");
        JsonNode attacker = register("attacker-" + suffix + "@test.example");

        String victimEmail = victim.get("email").asText();
        String attackerAuth = "Bearer " + attacker.get("accessToken").asText();
        String attackerId = attacker.get("id").asText();

        String renameBody = objectMapper.writeValueAsString(Map.of("email", victimEmail));
        mockMvc.perform(put("/user/update/" + attackerId)
                        .header("Authorization", attackerAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(renameBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        // Victim's account still resolves to exactly one user and keeps working.
        String victimAuth = "Bearer " + victim.get("accessToken").asText();
        mockMvc.perform(get("/user/get/" + victim.get("id").asText())
                        .header("Authorization", victimAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(victimEmail));
    }
}
