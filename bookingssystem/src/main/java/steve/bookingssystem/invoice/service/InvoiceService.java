package steve.bookingssystem.invoice.service;

import steve.bookingssystem.invoice.model.Invoice;
import steve.bookingssystem.invoice.model.InvoiceResponseDTO;
import steve.bookingssystem.payment.model.Payment;

import java.util.UUID;

public interface InvoiceService {
    Invoice generateForPayment(Payment payment);
    InvoiceResponseDTO getForBooking(UUID bookingId);
}
