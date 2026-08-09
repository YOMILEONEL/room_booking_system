package steve.bookingssystem.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import steve.bookingssystem.exception.InvalidTokenException;
import steve.bookingssystem.user.model.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final long ttlMinutes;

    public PasswordResetTokenServiceImpl(
            PasswordResetTokenRepository passwordResetTokenRepository,
            @Value("${security.jwt.passwordResetTokenTtlMinutes}") long ttlMinutes
    ) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.ttlMinutes = ttlMinutes;
    }

    @Override
    public PasswordResetToken create(User user) {
        Instant now = Instant.now();

        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(UUID.randomUUID().toString() + UUID.randomUUID());
        passwordResetToken.setUser(user);
        passwordResetToken.setCreatedAt(now);
        passwordResetToken.setExpiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES));
        passwordResetToken.setUsed(false);

        return passwordResetTokenRepository.save(passwordResetToken);
    }

    @Override
    public User consume(String token) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Unknown password reset token"));

        if (passwordResetToken.isUsed()) {
            throw new InvalidTokenException("Password reset token has already been used");
        }
        if (passwordResetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        passwordResetToken.setUsed(true);
        passwordResetTokenRepository.save(passwordResetToken);

        return passwordResetToken.getUser();
    }

    @Override
    public void deleteExpired() {
        passwordResetTokenRepository.deleteAll(passwordResetTokenRepository.findByExpiresAtBefore(Instant.now()));
    }
}
