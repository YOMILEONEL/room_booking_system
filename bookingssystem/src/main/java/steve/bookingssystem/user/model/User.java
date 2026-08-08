package steve.bookingssystem.user.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Data
@Entity
@Setter
@Getter
@Table(name= "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 6)
    private String password;

    @Enumerated(EnumType.STRING)
    private UserRole role= UserRole.MEMBER;

    @Enumerated(EnumType.STRING)
    private CustomerType customerType;

    private String organisationName;
    private String firstName;
    private String lastName;
    private String phoneNumber;

    public static UserDTO getUserDTO(User u){
        return new UserDTO(u.id, u.username, u.role, u.customerType, u.organisationName, u.firstName, u.lastName, u.phoneNumber);
    }


}
