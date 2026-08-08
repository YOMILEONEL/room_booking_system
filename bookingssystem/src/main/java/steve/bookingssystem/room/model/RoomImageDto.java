package steve.bookingssystem.room.model;

import java.util.UUID;

public record RoomImageDto(UUID id, String imageUrl, int position) {
    public static RoomImageDto from(RoomImage image) {
        return new RoomImageDto(image.getId(), image.getImageUrl(), image.getPosition());
    }
}
