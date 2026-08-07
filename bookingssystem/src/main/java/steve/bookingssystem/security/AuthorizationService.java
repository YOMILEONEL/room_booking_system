package steve.bookingssystem.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.repository.UserRepository;

@RequiredArgsConstructor
@Service
public class AuthorizationService {

    private final UserRepository userRepository;

    public User requireAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }

        User user = userRepository.findByUsername(auth.getName());
        if (user == null) {
            throw new AccessDeniedException("Not authenticated");
        }
        return user;
    }

    public Long requireAuthenticatedUserId() {
        return requireAuthenticatedUser().getId();
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    public void requireAdmin() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Admin only");
        }
    }

    public void requireOwnerOrAdmin(Long userId) {
        if (isAdmin()) {
            return;
        }
        if (userId == null || !userId.equals(requireAuthenticatedUserId())) {
            throw new AccessDeniedException("Not allowed");
        }
    }
}
