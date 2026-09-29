package steve.bookingssystem.assistant.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import steve.bookingssystem.assistant.model.AssistantMessage;
import steve.bookingssystem.assistant.model.AssistantSession;
import steve.bookingssystem.assistant.model.AssistantSessionDTO;
import steve.bookingssystem.assistant.repository.AssistantMessageRepository;
import steve.bookingssystem.assistant.repository.AssistantSessionRepository;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.user.model.User;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssistantSessionServiceImpl implements AssistantSessionService {

    private static final int PREVIEW_MAX_LENGTH = 80;

    @Autowired
    private AssistantSessionRepository assistantSessionRepository;
    @Autowired
    private AssistantMessageRepository assistantMessageRepository;
    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public AssistantSessionDTO create() {
        User user = authorizationService.requireAuthenticatedUser();

        AssistantSession session = new AssistantSession();
        session.setUser(user);
        session.setCreatedAt(Instant.now());
        assistantSessionRepository.save(session);

        return new AssistantSessionDTO(session.getId(), session.getCreatedAt(), null);
    }

    @Override
    public List<AssistantSessionDTO> getSessions() {
        User user = authorizationService.requireAuthenticatedUser();

        return assistantSessionRepository.findByUser_IdOrderByCreatedAtDesc(user.getId()).stream()
                .map(session -> new AssistantSessionDTO(session.getId(), session.getCreatedAt(), previewFor(session)))
                .toList();
    }

    private String previewFor(AssistantSession session) {
        Optional<AssistantMessage> first = assistantMessageRepository.findFirstBySession_IdOrderByCreatedAtAsc(session.getId());
        if (first.isEmpty()) {
            return null;
        }
        String content = first.get().getContent();
        return content.length() > PREVIEW_MAX_LENGTH ? content.substring(0, PREVIEW_MAX_LENGTH) + "…" : content;
    }

    // Called on logout for the session created at that login, if it was never actually chatted
    // in (see docs/ai-agent.md) - a silent no-op rather than an error for "already has messages"
    // or "not found"/"not mine", since logout must never fail because of this cleanup and the
    // frontend only ever passes the one session ID it knows is its own login-time session.
    @Override
    public void deleteIfUnused(UUID sessionId) {
        User user = authorizationService.requireAuthenticatedUser();

        assistantSessionRepository.findById(sessionId).ifPresent(session -> {
            if (!session.getUser().getId().equals(user.getId())) {
                return;
            }
            boolean hasMessages = assistantMessageRepository.findFirstBySession_IdOrderByCreatedAtAsc(sessionId).isPresent();
            if (!hasMessages) {
                assistantSessionRepository.delete(session);
            }
        });
    }

    @Override
    @Transactional
    public void delete(UUID sessionId) {
        User user = authorizationService.requireAuthenticatedUser();
        AssistantSession session = assistantSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Assistant session not found: " + sessionId));
        if (!session.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Not allowed");
        }

        assistantMessageRepository.deleteBySession_Id(sessionId);
        assistantSessionRepository.delete(session);
    }
}
