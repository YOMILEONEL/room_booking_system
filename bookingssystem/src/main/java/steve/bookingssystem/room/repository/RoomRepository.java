package steve.bookingssystem.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.model.Status;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    long countByRoomStatus(Status status);
    List<Room> findByActiveTrue();
}
