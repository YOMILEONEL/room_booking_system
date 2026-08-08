package steve.bookingssystem.user.model;

import lombok.Data;

import java.util.UUID;

@Data
public class UserDTO {
    private UUID id;
    private String username;
    private UserRole role;
    private CustomerType customerType;
    private String organisationName;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    public UserDTO(UUID id, String username, UserRole role, CustomerType customerType,
                   String organisationName, String firstName, String lastName, String phoneNumber) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.customerType = customerType;
        this.organisationName = organisationName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }


}
