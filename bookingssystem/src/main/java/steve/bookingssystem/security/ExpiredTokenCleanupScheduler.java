package steve.bookingssystem.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// docs/code-review.md, 2.3: refresh tokens and password reset tokens were never deleted after
// expiring, just left in the table forever (still queried by findByToken(...) on every request,
// and readable by anyone with DB access long after they stopped being usable).
@Component
@RequiredArgsConstructor
public class ExpiredTokenCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiredTokenCleanupScheduler.class);

    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenService passwordResetTokenService;

    @Scheduled(cron = "0 0 3 * * *")
    public void deleteExpiredTokens() {
        try {
            refreshTokenService.deleteExpired();
            passwordResetTokenService.deleteExpired();
        } catch (Exception ex) {
            // A failed cleanup run must not crash the scheduler thread for subsequent runs.
            log.error("Failed to delete expired refresh/password-reset tokens", ex);
        }
    }
}
