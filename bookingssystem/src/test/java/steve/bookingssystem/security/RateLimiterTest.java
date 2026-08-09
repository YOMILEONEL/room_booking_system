package steve.bookingssystem.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void allowsUpToTheConfiguredMaxWithinTheWindow() {
        RateLimiter rateLimiter = new RateLimiter();

        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.tryAcquire("key", 3, Duration.ofMinutes(1))).isTrue();
        }
    }

    @Test
    void rejectsOnceTheLimitIsExceeded() {
        RateLimiter rateLimiter = new RateLimiter();

        for (int i = 0; i < 3; i++) {
            rateLimiter.tryAcquire("key", 3, Duration.ofMinutes(1));
        }

        assertThat(rateLimiter.tryAcquire("key", 3, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    void differentKeysHaveIndependentBudgets() {
        RateLimiter rateLimiter = new RateLimiter();

        for (int i = 0; i < 3; i++) {
            rateLimiter.tryAcquire("key-a", 3, Duration.ofMinutes(1));
        }

        assertThat(rateLimiter.tryAcquire("key-a", 3, Duration.ofMinutes(1))).isFalse();
        assertThat(rateLimiter.tryAcquire("key-b", 3, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    void resetsAfterTheWindowExpires() {
        RateLimiter rateLimiter = new RateLimiter();
        Duration tinyWindow = Duration.ofMillis(50);

        for (int i = 0; i < 2; i++) {
            rateLimiter.tryAcquire("key", 2, tinyWindow);
        }
        assertThat(rateLimiter.tryAcquire("key", 2, tinyWindow)).isFalse();

        await(tinyWindow.toMillis() + 20);

        assertThat(rateLimiter.tryAcquire("key", 2, tinyWindow)).isTrue();
    }

    private void await(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
