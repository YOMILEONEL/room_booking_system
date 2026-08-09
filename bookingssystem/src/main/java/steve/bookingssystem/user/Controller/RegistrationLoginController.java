package steve.bookingssystem.user.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<?> loginUser(@RequestBody User user) {
        try{
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
            User user1 = userRepository.findByEmail(user.getEmail());
            String accessToken = jwtService.generateAccessToken(userDetailsService.loadUserByUsername(user1.getEmail()));
            RefreshToken refreshToken = refreshTokenService.create(user1);
            AuthResponse response = new AuthResponse(user1.getId(), user1.getEmail(), user1.getDisplayName(), user1.getRole(), user1.getCustomerType(), accessToken, refreshToken.getToken());
            return new ResponseEntity<Object>(response, HttpStatus.OK);
        }
        catch(Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("E-Mail-Adresse oder Passwort falsch");
        }
    }

    @PostMapping("/refresh")
    public AccessTokenResponse refresh(@RequestBody RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.validate(request.refreshToken());
        String accessToken = jwtService.generateAccessToken(userDetailsService.loadUserByUsername(refreshToken.getUser().getEmail()));
        return new AccessTokenResponse(accessToken);
    }

    @PostMapping("/logout")
    public void logout(@RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @PostMapping("/forgot-password")
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
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        User user = passwordResetTokenService.consume(request.token());
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
