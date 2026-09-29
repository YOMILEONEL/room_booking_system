package steve.bookingssystem.assistant.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class AssistantSessionDTO {

    private UUID id;
    private Instant createdAt;
    // First message of the session, trimmed for display in the session picker - null for an
    // empty (not yet used) session.
    private String preview;

    public AssistantSessionDTO(UUID id, Instant createdAt, String preview) {
        this.id = id;
        this.createdAt = createdAt;
        this.preview = preview;
    }
}
