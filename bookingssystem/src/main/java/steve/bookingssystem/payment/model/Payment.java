package steve.bookingssystem.payment.model;

import jakarta.persistence.*;
import lombok.Data;
import steve.bookingssystem.booking.model.Booking;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
