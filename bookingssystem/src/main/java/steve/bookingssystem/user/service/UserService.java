package steve.bookingssystem.user.service;


import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserDTO;

import java.util.List;
import java.util.UUID;

public interface UserService  {
    User addUser(User user);
    void updateUser(UUID id, User user);
    void deleteUser(UUID id);
    User getUserById(UUID id);
    List<UserDTO> getAllUsers();
}
