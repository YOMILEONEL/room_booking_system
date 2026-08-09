package steve.bookingssystem.invoice.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class InvoiceResponseDTO {

    private UUID id;
    private String invoiceNumber;
    private UUID bookingId;
    private BigDecimal amount;
    private String customerEmailSnapshot;
    private String roomNameSnapshot;
    private LocalDate invoiceDate;
    private Instant createdAt;

    public InvoiceResponseDTO(UUID id, String invoiceNumber, UUID bookingId, BigDecimal amount,
                               String customerEmailSnapshot, String roomNameSnapshot,
                               LocalDate invoiceDate, Instant createdAt) {
        this.id = id;
        this.invoiceNumber = invoiceNumber;
        this.bookingId = bookingId;
        this.amount = amount;
        this.customerEmailSnapshot = customerEmailSnapshot;
        this.roomNameSnapshot = roomNameSnapshot;
        this.invoiceDate = invoiceDate;
        this.createdAt = createdAt;
    }

    public static InvoiceResponseDTO from(Invoice invoice) {
        return new InvoiceResponseDTO(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getPayment().getBooking().getBookingId(),
                invoice.getAmount(),
                invoice.getCustomerEmailSnapshot(),
                invoice.getRoomNameSnapshot(),
                invoice.getInvoiceDate(),
                invoice.getCreatedAt()
        );
    }
}
