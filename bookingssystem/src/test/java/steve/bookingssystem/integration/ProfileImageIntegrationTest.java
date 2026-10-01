package steve.bookingssystem.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import steve.bookingssystem.storage.StorageService;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserRole;
import steve.bookingssystem.user.repository.UserRepository;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProfileImageIntegrationTest {

    private static final String URL = "http://s/storage/v1/object/public/b/avatars/x.png";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @MockitoBean
    private StorageService storageService;

    private record Account(UUID id, String token) {}

    private Account register() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "email", "avatar-" + UUID.randomUUID() + "@test.example",
                "password", "password123",
                "customerType", "KUNDE",
                "firstName", "Ada",
                "lastName", "Lovelace",
                "phoneNumber", "0123456789"));
        String response = mockMvc.perform(post("/api/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Ada"))
                .andExpect(jsonPath("$.profileImageUrl").value((Object) null))
                .andReturn().getResponse().getContentAsString();
        JsonNode auth = objectMapper.readTree(response);
        return new Account(UUID.fromString(auth.get("id").asText()), auth.get("accessToken").asText());
    }

    private Account registerAdmin() throws Exception {
        Account a = register();
        User u = userRepository.findById(a.id()).orElseThrow();
        u.setRole(UserRole.ADMIN);
        userRepository.save(u);
        return a;
    }

    private MockMultipartFile png() {
        return new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void ownAccount_uploadReturns200WithUrl_thenDeleteReturnsNull() throws Exception {
        Account me = register();
        when(storageService.uploadProfileImage(eq(me.id()), any())).thenReturn(URL);

        mockMvc.perform(multipart("/user/" + me.id() + "/profile-image").file(png())
                        .header("Authorization", "Bearer " + me.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(me.id().toString()))
                .andExpect(jsonPath("$.profileImageUrl").value(URL));

        mockMvc.perform(delete("/user/" + me.id() + "/profile-image")
                        .header("Authorization", "Bearer " + me.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value((Object) null));
    }

    @Test
    void foreignAccountAsCustomer_returns403() throws Exception {
        Account me = register();
        Account other = register();

        mockMvc.perform(multipart("/user/" + other.id() + "/profile-image").file(png())
                        .header("Authorization", "Bearer " + me.token()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/user/" + other.id() + "/profile-image")
                        .header("Authorization", "Bearer " + me.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnForeignAccount_returns200() throws Exception {
        Account admin = registerAdmin();
        Account other = register();
        when(storageService.uploadProfileImage(eq(other.id()), any())).thenReturn(URL);

        mockMvc.perform(multipart("/user/" + other.id() + "/profile-image").file(png())
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value(URL));
    }

    @Test
    void withoutLogin_returns401() throws Exception {
        mockMvc.perform(multipart("/user/" + UUID.randomUUID() + "/profile-image").file(png()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/user/" + UUID.randomUUID() + "/profile-image"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownId_returns404_forAdmin() throws Exception {
        Account admin = registerAdmin();
        UUID unknown = UUID.randomUUID();

        mockMvc.perform(multipart("/user/" + unknown + "/profile-image").file(png())
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/user/" + unknown + "/profile-image")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidFile_returns400WithErrorBody() throws Exception {
        Account me = register();
        when(storageService.uploadProfileImage(eq(me.id()), any()))
                .thenThrow(new IllegalArgumentException("Datei ist kein gültiges Bild"));
        MockMultipartFile html = new MockMultipartFile("file", "x.png", "image/png", "<html></html>".getBytes());

        mockMvc.perform(multipart("/user/" + me.id() + "/profile-image").file(html)
                        .header("Authorization", "Bearer " + me.token()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datei ist kein gültiges Bild"));
    }

    @Test
    void missingFilePart_returns400() throws Exception {
        Account me = register();

        mockMvc.perform(multipart("/user/" + me.id() + "/profile-image")
                        .header("Authorization", "Bearer " + me.token()))
                .andExpect(status().isBadRequest());
    }
}
