package steve.bookingssystem.user.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import steve.bookingssystem.security.JwtAuthFilter;
import steve.bookingssystem.security.RateLimitFilter;
import steve.bookingssystem.user.service.CustomUserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;

// Regression test for docs/code-review.md 2.4: the allowed CORS origin used to be a single
// hardcoded "http://localhost:3000", breaking login for any other frontend deployment target.
@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private JwtAuthFilter jwtAuthFilter;
    @Mock
    private RateLimitFilter rateLimitFilter;

    private CorsConfiguration corsConfigFor(String allowedOrigins) {
        SecurityConfig securityConfig = new SecurityConfig(userDetailsService, jwtAuthFilter, rateLimitFilter);
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", allowedOrigins);

        UrlBasedCorsConfigurationSource source =
                (UrlBasedCorsConfigurationSource) securityConfig.corsConfigurationSource();
        return source.getCorsConfigurations().get("/**");
    }

    @Test
    void defaultOrigin_isStillAllowed() {
        CorsConfiguration config = corsConfigFor("http://localhost:3000");

        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:3000");
    }

    @Test
    void commaSeparatedOrigins_areAllAllowedAndTrimmed() {
        CorsConfiguration config = corsConfigFor("https://spacio.example, https://staging.spacio.example");

        assertThat(config.getAllowedOrigins())
                .containsExactly("https://spacio.example", "https://staging.spacio.example");
    }
}
