package steve.bookingssystem.assistant.service;

import steve.bookingssystem.assistant.model.AssistantSessionDTO;

import java.util.List;
import java.util.UUID;

public interface AssistantSessionService {
    AssistantSessionDTO create();

    List<AssistantSessionDTO> getSessions();

    // No-op (not an error) if the session has any messages, or doesn't belong to the caller -
    // see AssistantSessionServiceImpl for why a silent no-op is the safer default here.
    void deleteIfUnused(UUID sessionId);

    // Explicit, user-initiated delete - unlike deleteIfUnused, this always deletes (messages
    // included) and errors (404/403) rather than silently no-op-ing, since the person clicked a
    // specific "delete this conversation" action and should know if it didn't happen.
    void delete(UUID sessionId);
}
