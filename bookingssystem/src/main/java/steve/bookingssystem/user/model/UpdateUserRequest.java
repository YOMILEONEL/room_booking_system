package steve.bookingssystem.user.model;

import jakarta.validation.constraints.Email;

public record UpdateUserRequest(@Email String email, String password, String currentPassword) {
}
