package steve.bookingssystem.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import steve.bookingssystem.exception.InvalidTokenException;
import steve.bookingssystem.user.model.User;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceImplTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private PasswordResetTokenServiceImpl passwordResetTokenService;

    @BeforeEach
    void setUp() {
        passwordResetTokenService = new PasswordResetTokenServiceImpl(passwordResetTokenRepository, 30L);
    }

    private PasswordResetToken token(boolean used, Instant expiresAt) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("alice");

        PasswordResetToken token = new PasswordResetToken();
        token.setToken("abc");
        token.setUser(user);
        token.setUsed(used);
        token.setExpiresAt(expiresAt);
        return token;
    }

    @Test
    void consume_throwsForUnknownToken() {
        when(passwordResetTokenRepository.findByToken("abc")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetTokenService.consume("abc"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void consume_throwsForAlreadyUsedToken() {
        when(passwordResetTokenRepository.findByToken("abc"))
                .thenReturn(Optional.of(token(true, Instant.now().plusSeconds(3600))));

        assertThatThrownBy(() -> passwordResetTokenService.consume("abc"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void consume_throwsForExpiredToken() {
        when(passwordResetTokenRepository.findByToken("abc"))
                .thenReturn(Optional.of(token(false, Instant.now().minusSeconds(60))));

        assertThatThrownBy(() -> passwordResetTokenService.consume("abc"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void consume_marksTokenUsedAndReturnsUser() {
        PasswordResetToken existing = token(false, Instant.now().plusSeconds(3600));
        when(passwordResetTokenRepository.findByToken("abc")).thenReturn(Optional.of(existing));

        User result = passwordResetTokenService.consume("abc");

        assertThat(result.getEmail()).isEqualTo("alice");
        assertThat(existing.isUsed()).isTrue();
        verify(passwordResetTokenRepository).save(existing);
    }
}
