package steve.bookingssystem.user.model;

public record UpdateUserRequest(String username, String password, String currentPassword) {
}
