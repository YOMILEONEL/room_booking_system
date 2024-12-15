package steve.bookingssystem.user.model;

import lombok.Data;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private UserRole role;

    public UserDTO(Long id, String username, UserRole role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }


}
