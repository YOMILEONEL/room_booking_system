package steve.bookingssystem.invoice.model;

import jakarta.persistence.*;
import lombok.Data;
import steve.bookingssystem.payment.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String invoiceNumber;

    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Column(nullable = false)
    private String customerEmailSnapshot;

    @Column(nullable = false)
    private String roomNameSnapshot;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    private Instant createdAt;

}
