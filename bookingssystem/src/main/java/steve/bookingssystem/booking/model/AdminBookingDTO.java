package steve.bookingssystem.booking.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class AdminBookingDTO {

    @NotNull
    private UUID roomId;

    @NotBlank
    private String customerEmail;

    @NotNull
    private LocalDate startTime;
    @NotNull
    private LocalDate endTime;

    private String discountCode;
}
