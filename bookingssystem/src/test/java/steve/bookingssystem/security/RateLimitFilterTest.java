package steve.bookingssystem.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// Regression test for docs/code-review.md 2.7: /api/login, /api/register and
// /api/forgot-password were open and unthrottled.
class RateLimitFilterTest {

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(new RateLimiter());
        // @Value isn't populated outside a Spring context - the field defaults to false, which
        // would make every test below a no-op without this.
        ReflectionTestUtils.setField(filter, "enabled", true);
    }

    private MockHttpServletRequest loginRequestFrom(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/login");
        request.setRemoteAddr(ip);
        return request;
    }

    @Test
    void allowsRequestsUnderTheLimit() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(loginRequestFrom("10.0.0.1"), response, chain);
            assertThat(response.getStatus()).isEqualTo(200); // MockHttpServletResponse defaults to 200
        }
    }

    @Test
    void blocksTheRequestOnceTheLimitIsExceeded() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        String ip = "10.0.0.2";

        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(loginRequestFrom(ip), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilterInternal(loginRequestFrom(ip), blocked, chain);

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentAsString()).contains("Zu viele Anfragen");
    }

    @Test
    void tracksEachIpIndependently() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(loginRequestFrom("10.0.0.3"), new MockHttpServletResponse(), chain);
        }

        MockHttpServletResponse otherIpResponse = new MockHttpServletResponse();
        filter.doFilterInternal(loginRequestFrom("10.0.0.4"), otherIpResponse, chain);

        assertThat(otherIpResponse.getStatus()).isEqualTo(200);
    }

    @Test
    void doesNotThrottleUnrelatedEndpoints() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/room/Get");
        request.setRemoteAddr("10.0.0.5");

        for (int i = 0; i < 50; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilterInternal(request, response, chain);
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
