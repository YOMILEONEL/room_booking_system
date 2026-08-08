package steve.bookingssystem.user.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 6) String password,
        @NotNull CustomerType customerType,
        String organisationName,
        String firstName,
        String lastName,
        @NotBlank String phoneNumber
) {
}
