package steve.bookingssystem.user.service;


import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserDTO;

import java.util.List;

public interface UserService  {
    User addUser(User user);
    void updateUser(Long id, User user);
    void deleteUser(Long id);
    User getUserById(Long id);
    List<UserDTO> getAllUsers();
}
