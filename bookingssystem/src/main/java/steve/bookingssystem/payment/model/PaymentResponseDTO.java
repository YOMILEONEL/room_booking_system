package steve.bookingssystem.payment.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class PaymentResponseDTO {

    private UUID id;
    private UUID bookingId;
    private BigDecimal amount;
    private PaymentStatus status;
    private String appliedDiscountCode;
    private Instant createdAt;
    private Instant paidAt;

    public PaymentResponseDTO(UUID id, UUID bookingId, BigDecimal amount, PaymentStatus status,
                               String appliedDiscountCode, Instant createdAt, Instant paidAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.amount = amount;
        this.status = status;
        this.appliedDiscountCode = appliedDiscountCode;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
    }

    public static PaymentResponseDTO from(Payment payment) {
        if (payment == null) {
            return null;
        }
        return new PaymentResponseDTO(
                payment.getId(),
                payment.getBooking().getBookingId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getAppliedDiscountCode(),
                payment.getCreatedAt(),
                payment.getPaidAt()
        );
    }
}
