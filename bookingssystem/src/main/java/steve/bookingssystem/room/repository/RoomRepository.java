package steve.bookingssystem.room.repository;

import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;
import steve.bookingssystem.room.model.Room;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
}
