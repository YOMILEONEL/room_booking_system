package steve.bookingssystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

// docs/code-review.md, 2.7: login, registration and forgot-password were open and unthrottled,
// with only a 6-character minimum password - realistic to brute-force online. This limits each
// of them per client IP; /api/refresh is deliberately excluded (it already requires possessing a
// valid, unguessable refresh token, so throttling it mainly risks locking out shared-IP users
// during normal, legitimate token-refresh traffic).
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private record Limit(int maxRequests, Duration window) {
    }

    private static final Map<String, Limit> LIMITS = Map.of(
            "/api/login", new Limit(10, Duration.ofMinutes(5)),
            "/api/register", new Limit(5, Duration.ofHours(1)),
            "/api/forgot-password", new Limit(5, Duration.ofHours(1))
    );

    private final RateLimiter rateLimiter;

    // Off by default in the "test" profile (see application-test.properties): the RateLimiter
    // bean is a singleton whose counters persist for the lifetime of the (often test-context-
    // shared) Spring context, so unrelated integration tests hammering /api/register etc. would
    // otherwise start tripping each other's limits. The throttling logic itself is covered by
    // RateLimiterTest/RateLimitFilterTest directly, without going through a shared context.
    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        Limit limit = LIMITS.get(request.getRequestURI());

        if (limit != null && "POST".equalsIgnoreCase(request.getMethod())) {
            String key = request.getRequestURI() + ":" + clientIp(request);
            if (!rateLimiter.tryAcquire(key, limit.maxRequests(), limit.window())) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Zu viele Anfragen. Bitte spaeter erneut versuchen.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    // Best-effort - trusts X-Forwarded-For if present (set by a reverse proxy in front of this
    // app), falling back to the direct connection's address otherwise. A spoofed header could
    // defeat this without a trusted proxy in front stripping/overwriting it, but that's the same
    // trust boundary every X-Forwarded-For consumer has.
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
