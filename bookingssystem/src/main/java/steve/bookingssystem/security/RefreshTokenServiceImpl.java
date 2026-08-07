package steve.bookingssystem.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import steve.bookingssystem.exception.InvalidTokenException;
import steve.bookingssystem.user.model.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long ttlDays;

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.jwt.refreshTokenTtlDays}") long ttlDays
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.ttlDays = ttlDays;
    }

    @Override
    public RefreshToken create(User user) {
        Instant now = Instant.now();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString() + UUID.randomUUID());
        refreshToken.setUser(user);
        refreshToken.setCreatedAt(now);
        refreshToken.setExpiresAt(now.plus(ttlDays, ChronoUnit.DAYS));
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken validate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Unknown refresh token"));

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        return refreshToken;
    }

    @Override
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }
}
