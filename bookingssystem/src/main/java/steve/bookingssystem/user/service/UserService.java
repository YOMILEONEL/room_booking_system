package steve.bookingssystem.user.service;


import org.springframework.http.ResponseEntity;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserDTO;

import java.util.List;

public interface UserService  {
    public User addUser(User user);
    public ResponseEntity<Void> updateUser(Long id ,User user);
    public ResponseEntity<Void> deleteUser(Long id);
    public User getUserById(Long id);
    public List<UserDTO> getAllUsers();
}
