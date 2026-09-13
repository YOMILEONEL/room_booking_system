package steve.bookingssystem.assistant.model;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class AssistantMessageDTO {

    private UUID id;
    private AssistantMessageRole role;
    private String content;
    private Instant createdAt;

    public AssistantMessageDTO(UUID id, AssistantMessageRole role, String content, Instant createdAt) {
        this.id = id;
        this.role = role;
        this.content = content;
        this.createdAt = createdAt;
    }

    public static AssistantMessageDTO from(AssistantMessage message) {
        return new AssistantMessageDTO(message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
    }
}
