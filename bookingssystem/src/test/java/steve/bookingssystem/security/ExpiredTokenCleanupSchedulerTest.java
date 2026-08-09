package steve.bookingssystem.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpiredTokenCleanupSchedulerTest {

    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @InjectMocks
    private ExpiredTokenCleanupScheduler scheduler;

    @Test
    void deleteExpiredTokens_callsBothServices() {
        scheduler.deleteExpiredTokens();

        verify(refreshTokenService).deleteExpired();
        verify(passwordResetTokenService).deleteExpired();
    }

    @Test
    void deleteExpiredTokens_doesNotPropagateFailures() {
        doThrow(new RuntimeException("DB unavailable")).when(refreshTokenService).deleteExpired();

        // A failed run must not crash the scheduler thread - it should just log and return, so
        // the next scheduled run still fires.
        assertThatCode(() -> scheduler.deleteExpiredTokens()).doesNotThrowAnyException();
    }
}
