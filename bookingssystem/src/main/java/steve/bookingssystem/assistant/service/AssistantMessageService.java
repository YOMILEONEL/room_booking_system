package steve.bookingssystem.assistant.service;

import steve.bookingssystem.assistant.model.AssistantMessageDTO;
import steve.bookingssystem.assistant.model.AssistantMessageRequest;

import java.util.List;
import java.util.UUID;

public interface AssistantMessageService {
    List<AssistantMessageDTO> getHistory(UUID sessionId);

    List<AssistantMessageDTO> addMessages(UUID sessionId, List<AssistantMessageRequest> messages);
}
