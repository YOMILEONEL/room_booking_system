package steve.bookingssystem.assistant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import steve.bookingssystem.assistant.model.AssistantMessage;
import steve.bookingssystem.assistant.model.AssistantMessageDTO;
import steve.bookingssystem.assistant.model.AssistantMessageRequest;
import steve.bookingssystem.assistant.model.AssistantSession;
import steve.bookingssystem.assistant.repository.AssistantMessageRepository;
import steve.bookingssystem.assistant.repository.AssistantSessionRepository;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.user.model.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class AssistantMessageServiceImpl implements AssistantMessageService {

    @Autowired
    private AssistantMessageRepository assistantMessageRepository;
    @Autowired
    private AssistantSessionRepository assistantSessionRepository;
    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public List<AssistantMessageDTO> getHistory(UUID sessionId) {
        AssistantSession session = requireOwnSession(sessionId);
        return assistantMessageRepository.findBySession_IdOrderByCreatedAtAsc(session.getId())
                .stream().map(AssistantMessageDTO::from).toList();
    }

    @Override
    @Transactional
    public List<AssistantMessageDTO> addMessages(UUID sessionId, List<AssistantMessageRequest> messages) {
        AssistantSession session = requireOwnSession(sessionId);
        User user = session.getUser();

        // All messages in one call (typically a USER question + its ASSISTANT reply) share the
        // request's timestamp resolution otherwise - plusNanos(index) keeps them in the order
        // the caller sent them without needing a separate sequence column.
        Instant base = Instant.now();
        List<AssistantMessage> saved = IntStream.range(0, messages.size()).mapToObj(index -> {
            AssistantMessageRequest req = messages.get(index);
            AssistantMessage message = new AssistantMessage();
            message.setUser(user);
            message.setSession(session);
            message.setRole(req.getRole());
            message.setContent(req.getContent());
            message.setCreatedAt(base.plusNanos(index));
            return assistantMessageRepository.save(message);
        }).toList();

        return saved.stream().map(AssistantMessageDTO::from).toList();
    }

    private AssistantSession requireOwnSession(UUID sessionId) {
        User user = authorizationService.requireAuthenticatedUser();
        AssistantSession session = assistantSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assistant session not found: " + sessionId));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not allowed");
        }
        return session;
    }
}
