package steve.bookingssystem.payment.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import steve.bookingssystem.booking.model.Booking;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Booking.payment (the inverse side) is not excluded, so Booking's generated toString()/
    // equals()/hashCode() still include this Payment - excluding the back-reference here (not
    // both sides) is enough to break the cycle: Payment's generated methods stop one hop short
    // of calling back into Booking, instead of recursing until StackOverflowError.
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 80)
    private String appliedDiscountCode;

    private Instant createdAt;
    private Instant paidAt;

}
