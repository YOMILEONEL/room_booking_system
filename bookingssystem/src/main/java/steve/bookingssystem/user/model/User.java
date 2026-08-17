package steve.bookingssystem.user.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
    @Email
    @Column(unique = true)
    private String email;

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
        return new UserDTO(u.id, u.email, u.role, u.customerType, u.organisationName, u.firstName, u.lastName, u.phoneNumber);
    }

    // Friendly name for the UI - "Vorname Nachname", organisation name, or the email as a last
    // resort. Deliberately does NOT require customerType to match: some accounts (older rows
    // predating this field, or ones created outside the normal /api/register flow) have real
    // firstName/lastName data but a null customerType - gating on customerType made those show
    // their email forever with no way to fix it, even though the actual name was right there.
    public String getDisplayName() {
        if (firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank()) {
            return firstName + " " + lastName;
        }
        if (organisationName != null && !organisationName.isBlank()) {
            return organisationName;
        }
        return email;
    }


}
