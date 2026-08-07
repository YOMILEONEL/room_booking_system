package steve.bookingssystem.user.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Entity
@Setter
@Getter
@Table(name= "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 6)
    private String password;

    private UserRole role= UserRole.MEMBER;

    public static UserDTO getUserDTO(User u){
        return new UserDTO(u.id, u.username, u.role);
    }


}
