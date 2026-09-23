package steve.bookingssystem.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

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
}
