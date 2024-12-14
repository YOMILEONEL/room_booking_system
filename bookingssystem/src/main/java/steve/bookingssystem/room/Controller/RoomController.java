package steve.bookingssystem.room.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.service.RoomService;

import java.util.List;

@RestController
@RequestMapping("/room")
public class RoomController {

    @Autowired
    private RoomService roomService;

    @GetMapping("/Get")
    public List<Room> getRooms() {
        return roomService.findAllRooms();
    }

    @PostMapping("/save")
    public Room saveRoom(@RequestBody Room room) {
        return roomService.saveRoom(room);
    }

    @DeleteMapping("delete/{id}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        return roomService.deleteRoom(id);
    }

    @PutMapping("update/{id}")
    public ResponseEntity<Room> updateRoom(@PathVariable Long id ,@RequestBody Room room) {
        return roomService.updateRoom(id, room);
    }

}