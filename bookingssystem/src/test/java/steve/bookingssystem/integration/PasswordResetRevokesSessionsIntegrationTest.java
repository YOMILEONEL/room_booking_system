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
import steve.bookingssystem.security.PasswordResetToken;
import steve.bookingssystem.security.PasswordResetTokenService;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.repository.UserRepository;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Regression test for docs/code-review.md 2.2: resetting a password used to leave every
// already-issued refresh token valid for up to 30 more days, so a stolen refresh token kept
// working even after the legitimate owner reset the password.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PasswordResetRevokesSessionsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordResetTokenService passwordResetTokenService;

    @Test
    void resetPassword_revokesPreviouslyIssuedRefreshTokens() throws Exception {
        String email = "reset-revoke-" + UUID.randomUUID() + "@test.example";
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "email", email,
                "password", "password123",
                "customerType", "KUNDE",
                "firstName", "Test",
                "lastName", "User",
                "phoneNumber", "0123456789"
        ));
        String registerResponse = mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode auth = objectMapper.readTree(registerResponse);
        String refreshToken = auth.get("refreshToken").asText();

        // The refresh token works before the reset.
        mockMvc.perform(post("/api/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk());

        User user = userRepository.findByEmail(email);
        PasswordResetToken resetToken = passwordResetTokenService.create(user);

        mockMvc.perform(post("/api/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", resetToken.getToken(),
                                "newPassword", "newpassword456"))))
                .andExpect(status().isOk());

        // The same refresh token - issued before the reset - must be rejected now.
        mockMvc.perform(post("/api/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }
}
