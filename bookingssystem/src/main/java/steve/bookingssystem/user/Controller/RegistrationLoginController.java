package steve.bookingssystem.user.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.security.JwtService;
import steve.bookingssystem.security.PasswordResetToken;
import steve.bookingssystem.security.PasswordResetTokenService;
import steve.bookingssystem.security.RefreshToken;
import steve.bookingssystem.security.RefreshTokenService;
import steve.bookingssystem.user.model.AccessTokenResponse;
import steve.bookingssystem.user.model.AuthResponse;
import steve.bookingssystem.user.model.CustomerType;
import steve.bookingssystem.user.model.ForgotPasswordRequest;
import steve.bookingssystem.user.model.LoginRequest;
import steve.bookingssystem.user.model.RefreshRequest;
import steve.bookingssystem.user.model.RegisterRequest;
import steve.bookingssystem.user.model.ResetPasswordRequest;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserRole;
import steve.bookingssystem.user.repository.UserRepository;
import steve.bookingssystem.user.service.CustomUserDetailsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Authentifizierung", description = "Registrierung, Login, Token-Refresh, Logout, Passwort-Reset - alles öffentlich, kein Bearer-Token nötig")
@SecurityRequirements
public class RegistrationLoginController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationLoginController.class);
    private static final String GENERIC_FORGOT_PASSWORD_MESSAGE =
            "Falls diese E-Mail-Adresse existiert, wurde ein Link zum Zuruecksetzen des Passworts erzeugt.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetTokenService passwordResetTokenService;

    @Value("${app.password-reset.log-token:false}")
    private boolean logResetToken;

    @PostMapping("/register")
    @Operation(summary = "Neues Konto registrieren",
            description = "Erstellt immer ein MEMBER-Konto (Kunde oder Organisation, je nach customerType). " +
                    "ADMIN-Konten können nur direkt in der Datenbank vergeben werden.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Konto angelegt, Access- und Refresh-Token geliefert"),
            @ApiResponse(responseCode = "400", description = "E-Mail bereits vergeben oder Pflichtfelder fehlen")
    })
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.email()) != null) {
            throw new IllegalArgumentException("Diese E-Mail-Adresse wird bereits verwendet.");
        }

        if (request.customerType() == CustomerType.ORGANISATION) {
            if (request.organisationName() == null || request.organisationName().isBlank()) {
                throw new IllegalArgumentException("Bitte den Namen der Organisation angeben.");
            }
        } else {
            if (request.firstName() == null || request.firstName().isBlank()
                    || request.lastName() == null || request.lastName().isBlank()) {
                throw new IllegalArgumentException("Bitte Vor- und Nachnamen angeben.");
            }
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        // Role is never taken from client input - self-registration always creates a MEMBER.
        // Promoting to ADMIN has to happen out-of-band (direct DB access); there is no
        // API path for it, so nobody can hand themselves elevated privileges at signup.
        user.setRole(UserRole.MEMBER);
        user.setCustomerType(request.customerType());
        user.setPhoneNumber(request.phoneNumber());
        if (request.customerType() == CustomerType.ORGANISATION) {
            user.setOrganisationName(request.organisationName());
        } else {
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
        }

        User savedUser = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(userDetailsService.loadUserByUsername(savedUser.getEmail()));
        RefreshToken refreshToken = refreshTokenService.create(savedUser);
        AuthResponse response = new AuthResponse(savedUser.getId(), savedUser.getEmail(), savedUser.getDisplayName(), savedUser.getRole(), savedUser.getCustomerType(), accessToken, refreshToken.getToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Anmelden", description = "Liefert bei gültigen Zugangsdaten ein Access- und ein Refresh-Token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login erfolgreich"),
            @ApiResponse(responseCode = "401", description = "E-Mail-Adresse oder Passwort falsch")
    })
    public ResponseEntity<AuthResponse> loginUser(@Valid @RequestBody LoginRequest request) {
        // Narrowed from catch(Exception e): that used to also swallow unrelated failures (DB
        // down, NPE) and mislabel them as "wrong password" - only AuthenticationException means
        // that. Anything else now propagates and surfaces as a real 500 instead of a lie.
        // AuthenticationException itself is handled centrally by GlobalExceptionHandler, giving
        // login the same {"error": "..."} response shape as every other endpoint (see
        // docs/code-review.md, 2.5 and the "three different error response shapes" 3.x finding).
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email());
        String accessToken = jwtService.generateAccessToken(userDetailsService.loadUserByUsername(user.getEmail()));
        RefreshToken refreshToken = refreshTokenService.create(user);
        AuthResponse response = new AuthResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getCustomerType(), accessToken, refreshToken.getToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Access-Token erneuern", description = "Tauscht ein gültiges Refresh-Token gegen ein neues Access-Token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Neues Access-Token geliefert"),
            @ApiResponse(responseCode = "401", description = "Refresh-Token ungültig, abgelaufen oder widerrufen")
    })
    public AccessTokenResponse refresh(@RequestBody RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.validate(request.refreshToken());
        String accessToken = jwtService.generateAccessToken(userDetailsService.loadUserByUsername(refreshToken.getUser().getEmail()));
        return new AccessTokenResponse(accessToken);
    }

    @PostMapping("/logout")
    @Operation(summary = "Abmelden", description = "Widerruft das übergebene Refresh-Token, das Access-Token bleibt bis zum Ablauf gültig.")
    @ApiResponse(responseCode = "200", description = "Refresh-Token widerrufen")
    public void logout(@RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Passwort-Reset anfordern",
            description = "Antwortet immer mit derselben generischen Nachricht, unabhängig davon, ob die " +
                    "E-Mail-Adresse existiert (Schutz vor Enumeration). Der Reset-Token wird aktuell nur " +
                    "geloggt, nicht gemailt (siehe docs/code-review.md).")
    @ApiResponse(responseCode = "200", description = "Generische Bestätigung (immer, unabhängig vom Ergebnis)")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email());
        if (user != null) {
            PasswordResetToken token = passwordResetTokenService.create(user);
            // Logging the raw token is a stand-in for actually emailing the reset link (no
            // SMTP/Resend configured) - anyone with log access could otherwise take over the
            // account within the token's validity window, so this is opt-in and off by default
            // (see app.password-reset.log-token / docs/code-review.md, 2.1).
            if (logResetToken) {
                log.info("Password reset token for user '{}': {}", user.getEmail(), token.getToken());
            } else {
                log.info("Password reset token generated for user '{}'", user.getEmail());
            }
        }
        // Same response regardless of whether the email exists - anti-enumeration.
        return ResponseEntity.ok(GENERIC_FORGOT_PASSWORD_MESSAGE);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Passwort mit Reset-Token setzen",
            description = "Widerruft dabei zusätzlich alle bestehenden Refresh-Tokens des Kontos, damit ein " +
                    "gestohlenes Refresh-Token nicht über den Passwort-Reset hinaus gültig bleibt.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Passwort geändert"),
            @ApiResponse(responseCode = "401", description = "Token ungültig, abgelaufen oder bereits verwendet")
    })
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        User user = passwordResetTokenService.consume(request.token());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        // Otherwise an attacker who stole a refresh token stays logged in for up to 30 days
        // even after the legitimate owner resets the password (see docs/code-review.md, 2.2).
        refreshTokenService.revokeAllForUser(user);
    }
}
