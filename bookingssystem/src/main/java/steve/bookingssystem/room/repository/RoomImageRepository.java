package steve.bookingssystem.room.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import steve.bookingssystem.room.model.RoomImage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, UUID> {
    List<RoomImage> findByRoom_IdOrderByPositionAsc(UUID roomId);
    Optional<RoomImage> findByIdAndRoom_Id(UUID id, UUID roomId);
    long countByRoom_Id(UUID roomId);
}
