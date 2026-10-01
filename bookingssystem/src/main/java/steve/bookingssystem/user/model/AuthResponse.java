package steve.bookingssystem.user.model;

import lombok.Data;

import java.util.UUID;

@Data
public class AuthResponse {
    private UUID id;
    private String email;
    private String displayName;
    private String firstName;
    private String profileImageUrl;
    private UserRole role;
    private CustomerType customerType;
    private String accessToken;
    private String refreshToken;

    public AuthResponse(UUID id, String email, String displayName, String firstName, String profileImageUrl, UserRole role, CustomerType customerType, String accessToken, String refreshToken) {
        this.firstName = firstName;
        this.profileImageUrl = profileImageUrl;
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.customerType = customerType;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
}
