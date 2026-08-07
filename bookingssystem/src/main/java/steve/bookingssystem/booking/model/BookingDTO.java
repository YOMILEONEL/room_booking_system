package steve.bookingssystem.booking.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingDTO {

    private Long bookingId;

    @NotNull
    private Long roomId;

    @NotNull
    private Long userId;

    @NotNull
    private LocalDate startTime;
    @NotNull
    private LocalDate endTime;

    private String discountCode;
}
