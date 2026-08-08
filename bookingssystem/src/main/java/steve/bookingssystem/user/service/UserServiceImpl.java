package steve.bookingssystem.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.user.model.UpdateUserRequest;
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
    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public User addUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public void updateUser(UUID id, UpdateUserRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        // Partial update: only touch fields the caller actually sent, so a
        // username-only change can never blank out the stored password (and
        // vice versa). Password must be hashed the same way registration does.
        if (request.username() != null && !request.username().isBlank()) {
            existingUser.setUsername(request.username());
        }
        if (request.password() != null && !request.password().isBlank()) {
            // Only enforced when users change their own password - an admin editing
            // someone else's account doesn't know that account's current password.
            boolean isSelfService = id.equals(authorizationService.requireAuthenticatedUserId());
            if (isSelfService) {
                if (request.currentPassword() == null || request.currentPassword().isBlank()
                        || !passwordEncoder.matches(request.currentPassword(), existingUser.getPassword())) {
                    throw new IllegalArgumentException("Aktuelles Passwort ist falsch");
                }
            }
            existingUser.setPassword(passwordEncoder.encode(request.password()));
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
