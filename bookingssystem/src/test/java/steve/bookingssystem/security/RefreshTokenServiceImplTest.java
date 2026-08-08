package steve.bookingssystem.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import steve.bookingssystem.exception.InvalidTokenException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, 30L);
    }

    private RefreshToken token(boolean revoked, Instant expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setToken("abc");
        token.setRevoked(revoked);
        token.setExpiresAt(expiresAt);
        return token;
    }

    @Test
    void validate_throwsForUnknownToken() {
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validate("abc"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validate_throwsForRevokedToken() {
        when(refreshTokenRepository.findByToken("abc"))
                .thenReturn(Optional.of(token(true, Instant.now().plusSeconds(3600))));

        assertThatThrownBy(() -> refreshTokenService.validate("abc"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validate_throwsForExpiredToken() {
        when(refreshTokenRepository.findByToken("abc"))
                .thenReturn(Optional.of(token(false, Instant.now().minusSeconds(60))));

        assertThatThrownBy(() -> refreshTokenService.validate("abc"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validate_returnsTokenWhenValid() {
        RefreshToken valid = token(false, Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(valid));

        assertThat(refreshTokenService.validate("abc")).isSameAs(valid);
    }

    @Test
    void revoke_marksTokenRevoked() {
        RefreshToken existing = token(false, Instant.now().plusSeconds(3600));
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(existing));

        refreshTokenService.revoke("abc");

        assertThat(existing.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(existing);
    }

    @Test
    void revoke_isNoOpForUnknownToken() {
        when(refreshTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        refreshTokenService.revoke("missing");

        verify(refreshTokenRepository, never()).save(any());
    }
}
