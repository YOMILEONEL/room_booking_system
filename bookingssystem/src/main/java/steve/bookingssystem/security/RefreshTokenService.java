package steve.bookingssystem.security;

import steve.bookingssystem.user.model.User;

public interface RefreshTokenService {
    RefreshToken create(User user);
    RefreshToken validate(String token);
    void revoke(String token);
}
