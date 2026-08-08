package steve.bookingssystem.security;

import jakarta.persistence.*;
import lombok.Data;
import steve.bookingssystem.user.model.User;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Instant createdAt;
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

}
