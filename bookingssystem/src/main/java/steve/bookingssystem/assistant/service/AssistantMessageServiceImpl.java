package steve.bookingssystem.assistant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import steve.bookingssystem.assistant.model.AssistantMessage;
import steve.bookingssystem.assistant.model.AssistantMessageDTO;
import steve.bookingssystem.assistant.model.AssistantMessageRequest;
import steve.bookingssystem.assistant.repository.AssistantMessageRepository;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.user.model.User;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

@Service
public class AssistantMessageServiceImpl implements AssistantMessageService {

    @Autowired
    private AssistantMessageRepository assistantMessageRepository;
    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public List<AssistantMessageDTO> getHistory() {
        User user = authorizationService.requireAuthenticatedUser();
        return assistantMessageRepository.findByUser_IdOrderByCreatedAtAsc(user.getId())
                .stream().map(AssistantMessageDTO::from).toList();
    }

    @Override
    @Transactional
    public List<AssistantMessageDTO> addMessages(List<AssistantMessageRequest> messages) {
        User user = authorizationService.requireAuthenticatedUser();

        // All messages in one call (typically a USER question + its ASSISTANT reply) share the
        // request's timestamp resolution otherwise - plusNanos(index) keeps them in the order
        // the caller sent them without needing a separate sequence column.
        Instant base = Instant.now();
        List<AssistantMessage> saved = IntStream.range(0, messages.size()).mapToObj(index -> {
            AssistantMessageRequest req = messages.get(index);
            AssistantMessage message = new AssistantMessage();
            message.setUser(user);
            message.setRole(req.getRole());
            message.setContent(req.getContent());
            message.setCreatedAt(base.plusNanos(index));
            return assistantMessageRepository.save(message);
        }).toList();

        return saved.stream().map(AssistantMessageDTO::from).toList();
    }
}
