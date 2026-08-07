package steve.bookingssystem.user.model;

import lombok.Data;

@Data
public class AuthResponse {
    private Long id;
    private String username;
    private UserRole role;
    private String accessToken;
    private String refreshToken;

    public AuthResponse(Long id, String username, UserRole role, String accessToken, String refreshToken) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
