package steve.bookingssystem.room.Controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.service.RoomService;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
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
    @ResponseStatus(HttpStatus.CREATED)
    public Room saveRoom(@Valid @RequestBody Room room) {
        return roomService.saveRoom(room);
    }

    @DeleteMapping("delete/{id}")
    public void deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
    }

    @PutMapping("update/{id}")
    public Room updateRoom(@PathVariable Long id, @Valid @RequestBody Room room) {
        return roomService.updateRoom(id, room);
    }

}
