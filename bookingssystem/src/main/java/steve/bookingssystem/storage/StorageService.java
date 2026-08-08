package steve.bookingssystem.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StorageService {
    String uploadRoomImage(UUID roomId, MultipartFile file);
}
