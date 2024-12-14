package steve.bookingssystem.booking.model;

import jakarta.persistence.*;
import lombok.Data;
import steve.bookingssystem.room.model.Room;

import java.security.Timestamp;

@Data
@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @OneToOne
    @JoinColumn(name = "roomId", nullable = false)
    private Room room;

    private Timestamp startTime;
    private Timestamp endTime;



}
