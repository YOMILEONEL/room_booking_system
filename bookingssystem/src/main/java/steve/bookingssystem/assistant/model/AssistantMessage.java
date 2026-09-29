package steve.bookingssystem.assistant.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import steve.bookingssystem.user.model.User;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "assistant_messages")
public class AssistantMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    // No nullable=false here on purpose: this column was added after messages already existed
    // in the database (from the earlier flat, non-session history), and a NOT NULL ALTER TABLE
    // against a non-empty table fails under Hibernate's ddl-auto=update - same reasoning as
    // Room.city/description. Every new message always sets it (see
    // AssistantMessageServiceImpl.addMessages); old rows keep session=null and are simply no
    // longer reachable through the session-scoped endpoints.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "sessionId")
    private AssistantSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssistantMessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Set per-message with increasing nanos within a batch insert (see
    // AssistantMessageServiceImpl.addMessages) so a user question and the assistant's reply
    // saved in the same request sort in conversation order rather than tying on the same
    // millisecond.
    @Column(nullable = false)
    private Instant createdAt;

}
