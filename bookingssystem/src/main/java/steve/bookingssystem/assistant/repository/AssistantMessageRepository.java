package steve.bookingssystem.assistant.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import steve.bookingssystem.assistant.model.AssistantMessage;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssistantMessageRepository extends JpaRepository<AssistantMessage, UUID> {
    List<AssistantMessage> findByUser_IdOrderByCreatedAtAsc(UUID userId);
}
