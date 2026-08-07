package steve.bookingssystem.room.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.repository.RoomRepository;
import steve.bookingssystem.security.AuthorizationService;

import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    public RoomRepository roomRepository;
    @Autowired
    public AuthorizationService authorizationService;

    @Override
    public Room saveRoom(Room room) {
        authorizationService.requireAdmin();
        return roomRepository.save(room);
    }

    @Override
    public Room findRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
    }

    @Override
    public List<Room> findAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    public void deleteRoom(Long id) {
        authorizationService.requireAdmin();
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        roomRepository.delete(room);
    }

    @Override
    public Room updateRoom(Long id, Room roomDetails) {
        authorizationService.requireAdmin();
        Room roomExist = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));

        roomExist.setName(roomDetails.getName());
        roomExist.setCapacity(roomDetails.getCapacity());
        roomExist.setLocation(roomDetails.getLocation());
        roomExist.setRoomStatus(roomDetails.getRoomStatus());
        return roomRepository.save(roomExist);
    }
}
