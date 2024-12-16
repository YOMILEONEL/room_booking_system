package steve.bookingssystem.booking.model;

import jakarta.persistence.*;
import lombok.Data;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.user.model.User;

import java.security.Timestamp;
import java.time.LocalDate;

@Data
@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @OneToOne
    @JoinColumn(name = "roomId", nullable = false)
    private Room room;

    @OneToOne
    @JoinColumn(name= "userId", nullable = false)
    private User user;

    private LocalDate startTime;
    private LocalDate endTime;



}
