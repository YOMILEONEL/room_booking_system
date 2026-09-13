package steve.bookingssystem.assistant.service;

import steve.bookingssystem.assistant.model.AssistantMessageDTO;
import steve.bookingssystem.assistant.model.AssistantMessageRequest;

import java.util.List;

public interface AssistantMessageService {
    List<AssistantMessageDTO> getHistory();

    List<AssistantMessageDTO> addMessages(List<AssistantMessageRequest> messages);
}
