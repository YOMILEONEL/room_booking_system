package steve.bookingssystem.room.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RoomResponseDTO(
        UUID id,
        String name,
        int capacity,
        String location,
        BigDecimal pricePerNight,
        BigDecimal effectivePricePerNight,
        Status roomStatus,
        String imageUrl,
        boolean active,
        LocalDate bookedUntil
) {
    public static RoomResponseDTO from(Room room, LocalDate bookedUntil, BigDecimal effectivePricePerNight) {
        return new RoomResponseDTO(
                room.getId(),
                room.getName(),
                room.getCapacity(),
                room.getLocation(),
                room.getPricePerNight(),
                effectivePricePerNight,
                room.getRoomStatus(),
                room.getImageUrl(),
                room.isActive(),
                bookedUntil
        );
    }
}
