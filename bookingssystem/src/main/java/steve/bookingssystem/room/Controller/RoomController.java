package steve.bookingssystem.room.Controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.model.RoomImageDto;
import steve.bookingssystem.room.model.RoomResponseDTO;
import steve.bookingssystem.room.service.RoomImageService;
import steve.bookingssystem.room.service.RoomService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/room")
public class RoomController {

    @Autowired
    private RoomService roomService;
    @Autowired
    private RoomImageService roomImageService;

    @GetMapping("/Get")
    public List<RoomResponseDTO> getRooms() {
        return roomService.findAllRooms();
    }

    @GetMapping("/{id}")
    public RoomResponseDTO getRoom(@PathVariable UUID id) {
        return roomService.findRoomById(id);
    }

    @PostMapping("/save")
    @ResponseStatus(HttpStatus.CREATED)
    public Room saveRoom(@Valid @RequestBody Room room) {
        return roomService.saveRoom(room);
    }

    @PutMapping("update/{id}")
    public Room updateRoom(@PathVariable UUID id, @Valid @RequestBody Room room) {
        return roomService.updateRoom(id, room);
    }

    @PutMapping("/{id}/activate")
    public Room activateRoom(@PathVariable UUID id) {
        return roomService.activate(id);
    }

    @PutMapping("/{id}/deactivate")
    public Room deactivateRoom(@PathVariable UUID id) {
        return roomService.deactivate(id);
    }

    @GetMapping("/{id}/images")
    public List<RoomImageDto> listImages(@PathVariable UUID id) {
        return roomImageService.list(id);
    }

    @PostMapping("/{id}/image")
    public RoomImageDto uploadImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return roomImageService.upload(id, file);
    }

    @DeleteMapping("/{id}/images/{imageId}")
    public void deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        roomImageService.delete(id, imageId);
    }

    @PutMapping("/{id}/images/reorder")
    public List<RoomImageDto> reorderImages(@PathVariable UUID id, @RequestBody List<UUID> orderedImageIds) {
        return roomImageService.reorder(id, orderedImageIds);
    }

}
