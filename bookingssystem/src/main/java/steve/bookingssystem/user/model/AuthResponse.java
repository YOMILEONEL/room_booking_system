package steve.bookingssystem.user.model;

import lombok.Data;

import java.util.UUID;

@Data
public class AuthResponse {
    private UUID id;
    private String username;
    private String displayName;
    private UserRole role;
    private CustomerType customerType;
    private String accessToken;
    private String refreshToken;

    public AuthResponse(UUID id, String username, String displayName, UserRole role, CustomerType customerType, String accessToken, String refreshToken) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        this.customerType = customerType;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
