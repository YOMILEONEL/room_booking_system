package steve.bookingssystem.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.storage.StorageService;
import steve.bookingssystem.user.model.UserDTO;
import steve.bookingssystem.user.model.UpdateUserRequest;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user(UUID id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("hashed");
        return user;
    }

    private static final String OLD_URL = "http://s/storage/v1/object/public/b/avatars/old.png";
    private static final String NEW_URL = "http://s/storage/v1/object/public/b/avatars/new.png";

    private MockMultipartFile png() {
        return new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
    }

    @Test
    void uploadProfileImage_replacesAndDeletesOldImage() {
        UUID id = UUID.randomUUID();
        User self = user(id, "a@test.example");
        self.setProfileImageUrl(OLD_URL);
        MockMultipartFile file = png();
        when(userRepository.findById(id)).thenReturn(Optional.of(self));
        when(storageService.uploadProfileImage(id, file)).thenReturn(NEW_URL);
        when(userRepository.save(self)).thenReturn(self);

        UserDTO dto = userService.uploadProfileImage(id, file);

        assertThat(dto.getProfileImageUrl()).isEqualTo(NEW_URL);
        assertThat(self.getProfileImageUrl()).isEqualTo(NEW_URL);
        verify(storageService).deleteProfileImageByPublicUrl(OLD_URL);
    }

    @Test
    void uploadProfileImage_firstImageDeletesNothing() {
        UUID id = UUID.randomUUID();
        User self = user(id, "a@test.example");
        MockMultipartFile file = png();
        when(userRepository.findById(id)).thenReturn(Optional.of(self));
        when(storageService.uploadProfileImage(id, file)).thenReturn(NEW_URL);
        when(userRepository.save(self)).thenReturn(self);

        userService.uploadProfileImage(id, file);

        verify(storageService, never()).deleteProfileImageByPublicUrl(anyString());
    }

    @Test
    void uploadProfileImage_uploadFailureLeavesRecordUnchanged() {
        UUID id = UUID.randomUUID();
        User self = user(id, "a@test.example");
        self.setProfileImageUrl(OLD_URL);
        MockMultipartFile file = png();
        when(userRepository.findById(id)).thenReturn(Optional.of(self));
        when(storageService.uploadProfileImage(id, file)).thenThrow(new IllegalArgumentException("kein gültiges Bild"));

        assertThatThrownBy(() -> userService.uploadProfileImage(id, file))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(self.getProfileImageUrl()).isEqualTo(OLD_URL);
        verify(userRepository, never()).save(any(User.class));
        verify(storageService, never()).deleteProfileImageByPublicUrl(anyString());
    }

    @Test
    void uploadProfileImage_deleteFailureIsSwallowed() {
        UUID id = UUID.randomUUID();
        User self = user(id, "a@test.example");
        self.setProfileImageUrl(OLD_URL);
        MockMultipartFile file = png();
        when(userRepository.findById(id)).thenReturn(Optional.of(self));
        when(storageService.uploadProfileImage(id, file)).thenReturn(NEW_URL);
        when(userRepository.save(self)).thenReturn(self);
        doThrow(new RuntimeException("s3 down")).when(storageService).deleteProfileImageByPublicUrl(OLD_URL);

        assertThat(userService.uploadProfileImage(id, file).getProfileImageUrl()).isEqualTo(NEW_URL);
    }

    @Test
    void uploadProfileImage_saveFailureDeletesNewObjectAndKeepsOld() {
        UUID id = UUID.randomUUID();
        User self = user(id, "a@test.example");
        self.setProfileImageUrl(OLD_URL);
        MockMultipartFile file = png();
        when(userRepository.findById(id)).thenReturn(Optional.of(self));
        when(storageService.uploadProfileImage(id, file)).thenReturn(NEW_URL);
        when(userRepository.save(self)).thenThrow(new IllegalStateException("db down"));

        assertThatThrownBy(() -> userService.uploadProfileImage(id, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");

        verify(storageService).deleteProfileImageByPublicUrl(NEW_URL);
        verify(storageService, never()).deleteProfileImageByPublicUrl(OLD_URL);
    }

    @Test
    void deleteUser_deletesProfileImage() {
        UUID id = UUID.randomUUID();
        User self = user(id, "a@test.example");
        self.setProfileImageUrl(OLD_URL);
        when(userRepository.findById(id)).thenReturn(Optional.of(self));

        userService.deleteUser(id);

        verify(userRepository).delete(self);
        verify(storageService).deleteProfileImageByPublicUrl(OLD_URL);
    }

    @Test
    void uploadProfileImage_unknownUserThrowsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.uploadProfileImage(id, png()))
                .isInstanceOf(steve.bookingssystem.exception.ResourceNotFoundException.class);
    }

    @Test
    void removeProfileImage_setsNullAndDeletesOldImage() {
        UUID id = UUID.randomUUID();
        User self = user(id, "a@test.example");
        self.setProfileImageUrl(OLD_URL);
        when(userRepository.findById(id)).thenReturn(Optional.of(self));
        when(userRepository.save(self)).thenReturn(self);

        UserDTO dto = userService.removeProfileImage(id);

        assertThat(dto.getProfileImageUrl()).isNull();
        assertThat(self.getProfileImageUrl()).isNull();
        verify(storageService).deleteProfileImageByPublicUrl(OLD_URL);
    }

    @Test
    void removeProfileImage_deleteFailureIsSwallowed() {
        UUID id = UUID.randomUUID();
        User self = user(id, "a@test.example");
        self.setProfileImageUrl(OLD_URL);
        when(userRepository.findById(id)).thenReturn(Optional.of(self));
        when(userRepository.save(self)).thenReturn(self);
        doThrow(new RuntimeException("s3 down")).when(storageService).deleteProfileImageByPublicUrl(OLD_URL);

        assertThat(userService.removeProfileImage(id).getProfileImageUrl()).isNull();
    }

    @Test
    void updateUser_rejectsEmailAlreadyTakenByAnotherUser() {
        UUID selfId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        User self = user(selfId, "member1@test.example");
        User admin = user(otherId, "admin@test.example");

        when(userRepository.findById(selfId)).thenReturn(Optional.of(self));
        when(userRepository.findByEmail("admin@test.example")).thenReturn(admin);

        assertThatThrownBy(() -> userService.updateUser(selfId, new UpdateUserRequest("admin@test.example", null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUser_allowsRenamingToAFreeEmail() {
        UUID selfId = UUID.randomUUID();
        User self = user(selfId, "member1@test.example");

        when(userRepository.findById(selfId)).thenReturn(Optional.of(self));
        when(userRepository.findByEmail("new@test.example")).thenReturn(null);

        userService.updateUser(selfId, new UpdateUserRequest("new@test.example", null, null, null, null, null));

        assertThat(self.getEmail()).isEqualTo("new@test.example");
        verify(userRepository).save(self);
    }

    @Test
    void updateUser_keepingTheSameEmailSkipsTheCollisionCheck() {
        UUID selfId = UUID.randomUUID();
        User self = user(selfId, "member1@test.example");

        when(userRepository.findById(selfId)).thenReturn(Optional.of(self));

        userService.updateUser(selfId, new UpdateUserRequest("member1@test.example", null, null, null, null, null));

        assertThat(self.getEmail()).isEqualTo("member1@test.example");
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void updateUser_allowsKeepingOwnEmailWhenLookupReturnsSelf() {
        UUID selfId = UUID.randomUUID();
        User self = user(selfId, "member1@test.example");

        when(userRepository.findById(selfId)).thenReturn(Optional.of(self));
        when(userRepository.findByEmail("renamed@test.example")).thenReturn(self);

        userService.updateUser(selfId, new UpdateUserRequest("renamed@test.example", null, null, null, null, null));

        assertThat(self.getEmail()).isEqualTo("renamed@test.example");
    }

    // Regression test: a legacy account created before firstName/lastName existed has both
    // blank, so User.getDisplayName() falls back to the email address everywhere in the UI
    // (NavBar included) - this is the only way to ever fix that, there's no other path to set
    // these fields after registration.
    @Test
    void updateUser_appliesFirstNameAndLastNameWhenProvided() {
        UUID selfId = UUID.randomUUID();
        User self = user(selfId, "member1@test.example");

        when(userRepository.findById(selfId)).thenReturn(Optional.of(self));

        userService.updateUser(selfId, new UpdateUserRequest(null, null, null, "Ada", "Lovelace", null));

        assertThat(self.getFirstName()).isEqualTo("Ada");
        assertThat(self.getLastName()).isEqualTo("Lovelace");
        verify(userRepository).save(self);
    }

    @Test
    void updateUser_appliesOrganisationNameWhenProvided() {
        UUID selfId = UUID.randomUUID();
        User self = user(selfId, "org1@test.example");

        when(userRepository.findById(selfId)).thenReturn(Optional.of(self));

        userService.updateUser(selfId, new UpdateUserRequest(null, null, null, null, null, "Acme GmbH"));

        assertThat(self.getOrganisationName()).isEqualTo("Acme GmbH");
    }

    @Test
    void updateUser_blankNameFieldsAreIgnoredNotBlankedOut() {
        UUID selfId = UUID.randomUUID();
        User self = user(selfId, "member1@test.example");
        self.setFirstName("Existing");
        self.setLastName("Name");

        when(userRepository.findById(selfId)).thenReturn(Optional.of(self));

        userService.updateUser(selfId, new UpdateUserRequest(null, null, null, "  ", "", null));

        assertThat(self.getFirstName()).isEqualTo("Existing");
        assertThat(self.getLastName()).isEqualTo("Name");
    }
}
