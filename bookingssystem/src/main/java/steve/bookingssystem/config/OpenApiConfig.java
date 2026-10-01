package steve.bookingssystem.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

// Publishes /v3/api-docs (raw OpenAPI JSON) and /swagger-ui/index.html (interactive docs) -
// springdoc builds both from the @Tag/@Operation/@ApiResponse annotations on the controllers
// plus the request/response DTOs, no manual spec file to keep in sync. Both paths are permitted
// without auth in SecurityConfig - browsing the docs shouldn't require a token, only the actual
// API calls made from "Try it out" do (via the Authorize button below).
@OpenAPIDefinition(
        info = @Info(
                title = "Spacio API",
                version = "v1",
                description = "REST-API des Spacio-Raumbuchungssystems (Spring Boot Backend). " +
                        "Alle Endpunkte außer /api/register, /api/login, /api/refresh, /api/logout, " +
                        "/api/forgot-password und /api/reset-password benötigen einen Bearer-Token " +
                        "(siehe \"Authorize\" oben rechts) - über /api/login erhalten.",
                contact = @Contact(name = "Spacio")
        ),
        servers = {
                @Server(url = "/", description = "Aktueller Server (relativ)")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Access-Token aus /api/login bzw. /api/refresh, ohne \"Bearer \"-Präfix."
)
@Configuration
public class OpenApiConfig {

    // Paths that SecurityConfig permits without a token - these never answer 401, so the
    // customizer below must not document it for them.
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/register", "/api/login", "/api/refresh", "/api/logout",
            "/api/forgot-password", "/api/reset-password", "/payment/stripe/webhook");

    // Every protected endpoint can answer 401 (missing/invalid/expired token, see
    // JsonAuthenticationEntryPoint), but annotating each controller method by hand is easy to
    // forget. This adds a "401" response to every operation on a non-public path that does not
    // declare one itself, so existing @ApiResponse annotations stay untouched and nothing is
    // listed twice.
    @Bean
    public OpenApiCustomizer unauthorizedResponseCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((path, item) -> {
                if (PUBLIC_PATHS.contains(path)) {
                    return;
                }
                item.readOperations().forEach(operation -> {
                    if (operation.getResponses() == null) {
                        operation.setResponses(new ApiResponses());
                    }
                    if (!operation.getResponses().containsKey("401")) {
                        operation.getResponses().addApiResponse("401",
                                new ApiResponse().description("Nicht eingeloggt oder Token ungültig"));
                    }
                });
            });
        };
    }
}
