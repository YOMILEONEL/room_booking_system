package steve.bookingssystem.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface StorageService {
    String uploadRoomImage(UUID roomId, MultipartFile file);

    String uploadProfileImage(UUID userId, MultipartFile file);

    /**
     * Deletes a profile image (avatar) only if the URL points to an object below
     * {@code avatars/} in our own bucket (no {@code ..} or backslash in the key);
     * otherwise does nothing. Not suitable for deleting other object kinds.
     */
    void deleteProfileImageByPublicUrl(String publicUrl);
}
