package steve.bookingssystem.room.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RoomResponseDTO(
        UUID id,
        String name,
        int capacity,
        Double sizeSquareMeters,
        String location,
        String city,
        String description,
        BigDecimal pricePerDay,
        BigDecimal effectivePricePerDay,
        Status roomStatus,
        String imageUrl,
        boolean active,
        LocalDate bookedUntil
) {
    public static RoomResponseDTO from(Room room, LocalDate bookedUntil, BigDecimal effectivePricePerDay) {
        return new RoomResponseDTO(
                room.getId(),
                room.getName(),
                room.getCapacity(),
                room.getSizeSquareMeters(),
                room.getLocation(),
                room.getCity(),
                room.getDescription(),
                room.getPricePerDay(),
                effectivePricePerDay,
                room.getRoomStatus(),
                room.getImageUrl(),
                room.isActive(),
                bookedUntil
        );
    }
}
