package steve.bookingssystem.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode paths() throws Exception {
        String body = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("paths");
    }

    @Test
    void protectedOperationWithoutAnnotation_documents401() throws Exception {
        JsonNode responses = paths().get("/room/Get").get("get").get("responses");
        assertTrue(responses.has("401"));
        assertEquals("Nicht eingeloggt oder Token ungültig", responses.get("401").get("description").asText());
    }

    @Test
    void publicOperations_doNotDocument401() throws Exception {
        JsonNode paths = paths();
        // /api/login declares its own 401 (wrong credentials) via @ApiResponse; the customizer
        // must leave that text alone instead of adding its generic "token invalid" one.
        assertEquals("E-Mail-Adresse oder Passwort falsch",
                paths.get("/api/login").get("post").get("responses").get("401").get("description").asText());
        assertFalse(paths.get("/api/logout").get("post").get("responses").has("401"));
        assertFalse(paths.get("/payment/stripe/webhook").get("post").get("responses").has("401"));
    }
}
