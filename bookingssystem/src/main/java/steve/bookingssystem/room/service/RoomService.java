package steve.bookingssystem.room.service;

import org.springframework.http.ResponseEntity;
import steve.bookingssystem.room.model.Room;

import java.util.List;

public interface RoomService {
    public Room saveRoom(Room room);
    public Room findRoomById(Long id);
    public List<Room> findAllRooms();
    public ResponseEntity<Void> deleteRoom(Long id);
    public ResponseEntity<Room> updateRoom(Long id, Room room);

}
