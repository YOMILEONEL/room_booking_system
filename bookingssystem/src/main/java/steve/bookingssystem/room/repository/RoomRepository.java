package steve.bookingssystem.room.repository;

import org.hibernate.type.descriptor.converter.spi.JpaAttributeConverter;
import org.springframework.data.jpa.repository.JpaRepository;
import steve.bookingssystem.room.model.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
}
