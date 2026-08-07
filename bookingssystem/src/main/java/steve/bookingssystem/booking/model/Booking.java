package steve.bookingssystem.booking.model;

import jakarta.persistence.*;
import lombok.Data;
import steve.bookingssystem.payment.model.Payment;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.user.model.User;

import java.time.LocalDate;

@Data
@Entity
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookingId;

    @ManyToOne
    @JoinColumn(name = "roomId", nullable = false)
    private Room room;

    @ManyToOne
    @JoinColumn(name= "userId", nullable = false)
    private User user;

    private LocalDate startTime;
    private LocalDate endTime;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

}
