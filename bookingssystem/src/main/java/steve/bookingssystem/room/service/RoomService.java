package steve.bookingssystem.room.service;

import steve.bookingssystem.room.model.Room;

import java.util.List;

public interface RoomService {
    Room saveRoom(Room room);
    Room findRoomById(Long id);
    List<Room> findAllRooms();
    void deleteRoom(Long id);
    Room updateRoom(Long id, Room room);
}
