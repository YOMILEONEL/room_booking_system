package steve.bookingssystem.user.Controller;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import steve.bookingssystem.security.JwtService;
import steve.bookingssystem.security.PasswordResetToken;
import steve.bookingssystem.security.PasswordResetTokenService;
import steve.bookingssystem.security.RefreshTokenService;
import steve.bookingssystem.user.model.ForgotPasswordRequest;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.repository.UserRepository;
import steve.bookingssystem.user.service.CustomUserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

// Regression test for docs/code-review.md 2.1: the raw password-reset token must never reach the
// logs unless app.password-reset.log-token is explicitly enabled (default: off).
@ExtendWith(MockitoExtension.class)
class RegistrationLoginControllerResetTokenLoggingTest {

    private static final String SECRET_TOKEN = "super-secret-reset-token-value";

    @Mock
    private UserRepository userRepository;
    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void attachLogAppender() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(RegistrationLoginController.class)).addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        ((Logger) LoggerFactory.getLogger(RegistrationLoginController.class)).detachAppender(logAppender);
    }

    private RegistrationLoginController controller(boolean logResetToken) {
        RegistrationLoginController controller = new RegistrationLoginController(
                userRepository, passwordEncoder, authenticationManager, jwtService,
                userDetailsService, refreshTokenService, passwordResetTokenService);
        ReflectionTestUtils.setField(controller, "logResetToken", logResetToken);
        return controller;
    }

    private void stubUserAndToken() {
        User user = new User();
        user.setEmail("someone");
        when(userRepository.findByEmail("someone")).thenReturn(user);

        PasswordResetToken token = new PasswordResetToken();
        token.setToken(SECRET_TOKEN);
        when(passwordResetTokenService.create(user)).thenReturn(token);
    }

    private String allLoggedMessages() {
        return logAppender.list.stream().map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + " | " + b);
    }

    @Test
    void byDefault_rawTokenIsNotLogged() {
        stubUserAndToken();

        controller(false).forgotPassword(new ForgotPasswordRequest("someone"));

        assertThat(allLoggedMessages()).doesNotContain(SECRET_TOKEN);
    }

    @Test
    void whenExplicitlyEnabled_rawTokenIsLogged() {
        stubUserAndToken();

        controller(true).forgotPassword(new ForgotPasswordRequest("someone"));

        assertThat(allLoggedMessages()).contains(SECRET_TOKEN);
    }
}
