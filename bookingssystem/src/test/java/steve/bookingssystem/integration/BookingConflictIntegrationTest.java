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
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserRole;
import steve.bookingssystem.user.repository.UserRepository;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingConflictIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

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

    /**
     * Registration always creates a MEMBER (no client-controlled role, see
     * RegistrationLoginController.registerUser) - promoting to ADMIN only happens
     * out-of-band, simulated here via direct repository access instead of the API.
     * The already-issued access token still picks this up: JwtAuthFilter re-resolves
     * authorities from the DB by email on every request, it doesn't bake the role
     * into the token itself.
     */
    private void promoteToAdmin(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setRole(UserRole.ADMIN);
        userRepository.save(user);
    }

    @Test
    void secondOverlappingBooking_isRejectedWithConflict() throws Exception {
        String suffix = UUID.randomUUID().toString();
        JsonNode admin = register("admin-" + suffix + "@test.example");
        promoteToAdmin(UUID.fromString(admin.get("id").asText()));
        JsonNode member = register("member-" + suffix + "@test.example");

        String roomBody = objectMapper.writeValueAsString(Map.of(
                "name", "Konferenzraum " + suffix,
                "capacity", 8,
                "location", "1. OG",
                "city", "Musterstadt",
                "description", "Ein heller Konferenzraum fuer Tests.",
                "pricePerDay", 100
        ));
        String roomResponse = mockMvc.perform(post("/room/save")
                        .header("Authorization", "Bearer " + admin.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String roomId = objectMapper.readTree(roomResponse).get("id").asText();

        String memberAuth = "Bearer " + member.get("accessToken").asText();
        String memberId = member.get("id").asText();

        String firstBooking = objectMapper.writeValueAsString(Map.of(
                "roomId", roomId,
                "userId", memberId,
                "startTime", "2026-03-01",
                "endTime", "2026-03-03"
        ));
        mockMvc.perform(post("/booking/add")
                        .header("Authorization", memberAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstBooking))
                .andExpect(status().isCreated());

        String overlappingBooking = objectMapper.writeValueAsString(Map.of(
                "roomId", roomId,
                "userId", memberId,
                "startTime", "2026-03-02",
                "endTime", "2026-03-04"
        ));
        mockMvc.perform(post("/booking/add")
                        .header("Authorization", memberAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlappingBooking))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void bookingWithInvalidDiscountCode_leavesNoGhostBooking() throws Exception {
        String suffix = UUID.randomUUID().toString();
        JsonNode admin = register("admin-" + suffix + "@test.example");
        promoteToAdmin(UUID.fromString(admin.get("id").asText()));
        JsonNode member = register("member-" + suffix + "@test.example");

        String roomBody = objectMapper.writeValueAsString(Map.of(
                "name", "Konferenzraum " + suffix,
                "capacity", 8,
                "location", "1. OG",
                "city", "Musterstadt",
                "description", "Ein heller Konferenzraum fuer Tests.",
                "pricePerDay", 100
        ));
        String roomResponse = mockMvc.perform(post("/room/save")
                        .header("Authorization", "Bearer " + admin.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String roomId = objectMapper.readTree(roomResponse).get("id").asText();

        String memberAuth = "Bearer " + member.get("accessToken").asText();
        String memberId = member.get("id").asText();

        // Unknown discount code makes createBookingFor throw *after* the booking/payment
        // inserts - without @Transactional this used to leave a paymentless "ghost booking"
        // that permanently blocked the room (see BookingServiceImpl.createBookingFor).
        String bookingWithBadCode = objectMapper.writeValueAsString(Map.of(
                "roomId", roomId,
                "userId", memberId,
                "startTime", "2026-04-01",
                "endTime", "2026-04-03",
                "discountCode", "DOES-NOT-EXIST"
        ));
        mockMvc.perform(post("/booking/add")
                        .header("Authorization", memberAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bookingWithBadCode))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/booking/getAll")
                        .header("Authorization", memberAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
