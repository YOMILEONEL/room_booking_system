package steve.bookingssystem.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import steve.bookingssystem.storage.StorageService;
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

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private StorageService storageService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public void updateUser(UUID id, UpdateUserRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        // Partial update: only touch fields the caller actually sent, so an
        // email-only change can never blank out the stored password (and
        // vice versa). Password must be hashed the same way registration does.
        if (request.email() != null && !request.email().isBlank()
                && !request.email().equals(existingUser.getEmail())) {
            // Without this check, any member could rename themselves to an admin's email:
            // findByEmail then returns two rows for that admin, breaking their login/auth
            // on every request (see docs/code-review.md, 1.9). The unique DB constraint on
            // User.email is the hard backstop; this just turns the collision into a clean
            // error instead of a raw constraint violation.
            User collision = userRepository.findByEmail(request.email());
            if (collision != null && !collision.getId().equals(id)) {
                throw new IllegalArgumentException("Diese E-Mail-Adresse wird bereits verwendet.");
            }
            existingUser.setEmail(request.email());
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
        // Same partial-update rule as email/password: only overwrite a field the caller
        // actually sent. Lets an account originally created without these (e.g. a legacy
        // row from before firstName/lastName existed) fill them in later - getDisplayName()
        // falls back to the email address whenever they're blank, so this is the only way
        // for such an account to ever show a real name instead of its email in the UI.
        if (request.firstName() != null && !request.firstName().isBlank()) {
            existingUser.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            existingUser.setLastName(request.lastName());
        }
        if (request.organisationName() != null && !request.organisationName().isBlank()) {
            existingUser.setOrganisationName(request.organisationName());
        }
        userRepository.save(existingUser);
    }

    @Override
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        String imageUrl = user.getProfileImageUrl();
        userRepository.delete(user);
        deleteOldImageBestEffort(imageUrl);
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

    @Override
    public UserDTO uploadProfileImage(UUID id, MultipartFile file) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        String oldUrl = user.getProfileImageUrl();
        // If the upload fails the exception propagates and the record stays untouched.
        String newUrl = storageService.uploadProfileImage(id, file);
        user.setProfileImageUrl(newUrl);
        // Known limitation: two parallel uploads for the same user can still orphan one
        // object (both read the same oldUrl). Deliberately not solved with locks.
        User saved;
        try {
            saved = userRepository.save(user);
        } catch (RuntimeException e) {
            // Record not updated: the new object would be orphaned, the old one stays valid.
            deleteOldImageBestEffort(newUrl);
            throw e;
        }
        deleteOldImageBestEffort(oldUrl);
        return User.getUserDTO(saved);
    }

    @Override
    public UserDTO removeProfileImage(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        String oldUrl = user.getProfileImageUrl();
        user.setProfileImageUrl(null);
        User saved = userRepository.save(user);
        deleteOldImageBestEffort(oldUrl);
        return User.getUserDTO(saved);
    }

    private void deleteOldImageBestEffort(String oldUrl) {
        if (oldUrl == null) {
            return;
        }
        try {
            storageService.deleteProfileImageByPublicUrl(oldUrl);
        } catch (Exception e) {
            log.warn("Profilbild konnte nicht gelöscht werden: {}", e.getMessage());
        }
    }
}
