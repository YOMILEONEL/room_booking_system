package steve.bookingssystem.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.repository.UserRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    private static final UUID ALICE_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();

    @Mock
    private UserRepository userRepository;

    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        authorizationService = new AuthorizationService(userRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(String email, String... roles) {
        List<GrantedAuthority> authorities = List.of(roles).stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null, authorities));
    }

    @Test
    void requireOwnerOrAdmin_allowsResourceOwner() {
        authenticateAs("alice", "ROLE_MEMBER");
        User alice = new User();
        alice.setId(ALICE_ID);
        alice.setEmail("alice");
        lenient().when(userRepository.findByEmail("alice")).thenReturn(alice);

        assertThatCode(() -> authorizationService.requireOwnerOrAdmin(ALICE_ID)).doesNotThrowAnyException();
    }

    @Test
    void requireOwnerOrAdmin_allowsAdminForAnyUser() {
        authenticateAs("admin", "ROLE_ADMIN");

        assertThatCode(() -> authorizationService.requireOwnerOrAdmin(OTHER_USER_ID)).doesNotThrowAnyException();
    }

    @Test
    void requireOwnerOrAdmin_rejectsDifferentNonAdminUser() {
        authenticateAs("alice", "ROLE_MEMBER");
        User alice = new User();
        alice.setId(ALICE_ID);
        alice.setEmail("alice");
        when(userRepository.findByEmail("alice")).thenReturn(alice);

        assertThatThrownBy(() -> authorizationService.requireOwnerOrAdmin(OTHER_USER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }
}
