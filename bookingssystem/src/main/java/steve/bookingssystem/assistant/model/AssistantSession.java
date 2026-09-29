package steve.bookingssystem.assistant.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import steve.bookingssystem.user.model.User;

import java.time.Instant;
import java.util.UUID;

// One row per chat session with the assistant - created automatically at login (see
// AssistantSessionServiceImpl.create, called from the frontend's NextAuth jwt() callback) so a
// person always lands on a fresh, empty session, and can pick an older one from
// GET /assistant/sessions instead. AssistantMessage.session groups messages into these.
@Data
@Entity
@Table(name = "assistant_sessions")
public class AssistantSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant createdAt;

}
