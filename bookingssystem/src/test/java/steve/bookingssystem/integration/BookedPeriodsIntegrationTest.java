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
class BookedPeriodsIntegrationTest {

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

    private String createRoom(String adminAuth, String suffix) throws Exception {
        String roomBody = objectMapper.writeValueAsString(Map.of(
                "name", "Raum " + suffix,
                "capacity", 8,
                "location", "1. OG",
                "city", "Musterstadt",
                "description", "Ein heller Raum fuer Tests.",
                "pricePerDay", 100
        ));
        String response = mockMvc.perform(post("/room/save")
                        .header("Authorization", adminAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(roomBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void bookedPeriods_securityValidationAndPrivacy() throws Exception {
        String suffix = UUID.randomUUID().toString();
        JsonNode admin = register("admin-" + suffix + "@test.example");
        User adminUser = userRepository.findById(UUID.fromString(admin.get("id").asText())).orElseThrow();
        adminUser.setRole(UserRole.ADMIN);
        userRepository.save(adminUser);
        JsonNode member = register("member-" + suffix + "@test.example");
        String adminAuth = "Bearer " + admin.get("accessToken").asText();
        String memberAuth = "Bearer " + member.get("accessToken").asText();
        String roomId = createRoom(adminAuth, suffix);

        mockMvc.perform(post("/booking/add")
                        .header("Authorization", memberAuth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "roomId", roomId,
                                "userId", member.get("id").asText(),
                                "startTime", "2030-03-05",
                                "endTime", "2030-03-07"))))
                .andExpect(status().isCreated());

        String url = "/room/" + roomId + "/booked-periods";

        // Without login: 401 (JsonAuthenticationEntryPoint, same as every other protected endpoint)
        mockMvc.perform(get(url).param("from", "2030-03-01").param("to", "2030-03-31"))
                .andExpect(status().isUnauthorized());

        // 200 for any logged-in user, only startTime/endTime exposed
        mockMvc.perform(get(url).header("Authorization", memberAuth)
                        .param("from", "2030-03-01").param("to", "2030-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].startTime").value("2030-03-05"))
                .andExpect(jsonPath("$[0].endTime").value("2030-03-07"))
                .andExpect(jsonPath("$[0].length()").value(2));

        // window outside the booking -> empty
        mockMvc.perform(get(url).header("Authorization", memberAuth)
                        .param("from", "2030-04-01").param("to", "2030-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        // 400: to before from
        mockMvc.perform(get(url).header("Authorization", memberAuth)
                        .param("from", "2030-03-10").param("to", "2030-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        // 400: span > 366 days
        mockMvc.perform(get(url).header("Authorization", memberAuth)
                        .param("from", "2030-01-01").param("to", "2031-01-03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());

        // 400: unparsable date
        mockMvc.perform(get(url).header("Authorization", memberAuth).param("from", "kein-datum"))
                .andExpect(status().isBadRequest());

        // 404: unknown room
        mockMvc.perform(get("/room/" + UUID.randomUUID() + "/booked-periods").header("Authorization", memberAuth))
                .andExpect(status().isNotFound());
    }
}
