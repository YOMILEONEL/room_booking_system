package steve.bookingssystem.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserDTO;
import steve.bookingssystem.user.repository.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public User addUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public void updateUser(UUID id, User newUser) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        // Partial update: only touch fields the caller actually sent, so a
        // username-only change can never blank out the stored password (and
        // vice versa). Password must be hashed the same way registration does.
        if (newUser.getUsername() != null && !newUser.getUsername().isBlank()) {
            existingUser.setUsername(newUser.getUsername());
        }
        if (newUser.getPassword() != null && !newUser.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(newUser.getPassword()));
        }
        userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        userRepository.delete(user);
    }

    @Override
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream().map(User::getUserDTO).collect(Collectors.toList());
    }
}
