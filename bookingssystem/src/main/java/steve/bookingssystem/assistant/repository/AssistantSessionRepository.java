package steve.bookingssystem.assistant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import steve.bookingssystem.assistant.model.AssistantSession;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssistantSessionRepository extends JpaRepository<AssistantSession, UUID> {
    List<AssistantSession> findByUser_IdOrderByCreatedAtDesc(UUID userId);
}
