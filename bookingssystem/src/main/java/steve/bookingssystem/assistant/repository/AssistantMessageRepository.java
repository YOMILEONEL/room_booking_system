package steve.bookingssystem.assistant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import steve.bookingssystem.assistant.model.AssistantMessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, UUID> {
    List<AssistantMessage> findBySession_IdOrderByCreatedAtAsc(UUID sessionId);

    // Used to decide whether a session counts as "used" - e.g. AssistantSessionServiceImpl.
    // deleteIfUnused only removes a session with no messages at all.
    Optional<AssistantMessage> findFirstBySession_IdOrderByCreatedAtAsc(UUID sessionId);

    // No DB-level ON DELETE CASCADE on the sessionId FK (Hibernate's ddl-auto=update doesn't
    // manage that), so AssistantSessionServiceImpl.delete() calls this itself before deleting
    // the session, inside the same transaction.
    void deleteBySession_Id(UUID sessionId);
}
