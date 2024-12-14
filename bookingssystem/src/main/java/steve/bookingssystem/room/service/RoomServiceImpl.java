package steve.bookingssystem.room.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.repository.RoomRepository;

import java.util.List;
import java.util.Optional;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    public RoomRepository roomRepository;

    @Override
    public Room saveRoom(Room room) {
        return roomRepository.save(room);
    }


    @Override
    public Room findRoomById(@PathVariable Long id) {
        Optional<Room> room = roomRepository.findById(id);
        if (room.isPresent()) {
            return room.get();
        }
        return null;
    }

    @Override
    public List<Room> findAllRooms() {
        return roomRepository.findAll();
    }

    @Override
        public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        Optional<Room> room = roomRepository.findById(id);

        if (room.isPresent()) {
            roomRepository.delete(room.get());
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    public ResponseEntity<Room> updateRoom(@PathVariable Long id, @RequestBody Room roomDetails) {
        Optional<Room> room = roomRepository.findById(id);

        if (room.isPresent()) {
            Room roomExist = room.get();
            roomExist.setName(roomDetails.getName());
            roomExist.setCapacity(roomDetails.getCapacity());
            roomExist.setLocation(roomDetails.getLocation());
            roomRepository.save(roomExist);

            return new ResponseEntity<>(roomExist, HttpStatus.OK);
            }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
