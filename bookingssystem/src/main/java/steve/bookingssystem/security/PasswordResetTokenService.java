package steve.bookingssystem.security;

import steve.bookingssystem.user.model.User;

public interface PasswordResetTokenService {
    PasswordResetToken create(User user);
    User consume(String token);
}
