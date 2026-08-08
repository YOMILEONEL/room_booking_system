package steve.bookingssystem.booking.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class BookingDTO {

    private UUID bookingId;

    @NotNull
    private UUID roomId;

    @NotNull
    private UUID userId;

    @NotNull
    private LocalDate startTime;
    @NotNull
    private LocalDate endTime;

    private String discountCode;
}
