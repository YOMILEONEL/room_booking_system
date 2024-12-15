package steve.bookingssystem.booking.model;

import jakarta.persistence.*;
import lombok.Data;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.user.model.User;

import java.security.Timestamp;
import java.time.LocalDate;

@Data
public class BookingDTO {

    private Long bookingId;


    private Long roomId;

    private Long userId;

    private LocalDate startTime;
    private LocalDate endTime;
}
