package steve.bookingssystem.room.service;

import steve.bookingssystem.room.model.BookedPeriodDTO;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.model.RoomResponseDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RoomService {
    Room saveRoom(Room room);
    RoomResponseDTO findRoomById(UUID id);
    List<RoomResponseDTO> findAllRooms();
    Room updateRoom(UUID id, Room room);
    Room activate(UUID id);
    Room deactivate(UUID id);
    List<BookedPeriodDTO> getBookedPeriods(UUID roomId, LocalDate from, LocalDate to);
}
