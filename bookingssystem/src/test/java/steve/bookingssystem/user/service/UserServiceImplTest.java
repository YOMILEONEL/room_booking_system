package steve.bookingssystem.user.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.user.model.UpdateUserRequest;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.repository.UserRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private UserServiceImpl userService;

    private User user(UUID id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword("hashed");
        return user;
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
