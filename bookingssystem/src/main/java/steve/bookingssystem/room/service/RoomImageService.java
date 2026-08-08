package steve.bookingssystem.room.service;

import org.springframework.web.multipart.MultipartFile;
import steve.bookingssystem.room.model.RoomImageDto;

import java.util.List;
import java.util.UUID;

public interface RoomImageService {
    List<RoomImageDto> list(UUID roomId);
    RoomImageDto upload(UUID roomId, MultipartFile file);
    void delete(UUID roomId, UUID imageId);
    List<RoomImageDto> reorder(UUID roomId, List<UUID> orderedImageIds);
}
