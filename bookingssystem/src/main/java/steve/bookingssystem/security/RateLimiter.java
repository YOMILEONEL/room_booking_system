package steve.bookingssystem.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// In-memory, fixed-window rate limiter (docs/code-review.md, 2.7). Deliberately simple: this app
// runs as a single backend instance, so an in-process counter is enough - it would need to move
// to a shared store (e.g. Redis) the moment this runs behind more than one instance.
@Component
public class RateLimiter {

    private static final class Window {
        private long windowStartMillis;
        private int count;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * @return true if the call is allowed, false if {@code key} already used up its quota for
     * the current window.
     */
    public boolean tryAcquire(String key, int maxRequests, Duration windowDuration) {
        Window window = windows.computeIfAbsent(key, k -> new Window());
        long now = System.currentTimeMillis();

        synchronized (window) {
            if (now - window.windowStartMillis > windowDuration.toMillis()) {
                window.windowStartMillis = now;
                window.count = 0;
            }
            window.count++;
            return window.count <= maxRequests;
        }
    }
}
