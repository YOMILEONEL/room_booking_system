package steve.bookingssystem.user.service;


import org.springframework.web.multipart.MultipartFile;
import steve.bookingssystem.user.model.UpdateUserRequest;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserDTO;

import java.util.List;
import java.util.UUID;

public interface UserService  {
    void updateUser(UUID id, UpdateUserRequest request);
    void deleteUser(UUID id);
    User getUserById(UUID id);
    List<UserDTO> getAllUsers();
    UserDTO uploadProfileImage(UUID id, MultipartFile file);
    UserDTO removeProfileImage(UUID id);
}
