package steve.bookingssystem.invoice.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.invoice.model.Invoice;
import steve.bookingssystem.invoice.model.InvoiceResponseDTO;
import steve.bookingssystem.invoice.repository.InvoiceRepository;
import steve.bookingssystem.payment.model.Payment;
import steve.bookingssystem.payment.repository.PaymentRepository;
import steve.bookingssystem.security.AuthorizationService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public Invoice generateForPayment(Payment payment) {
        String prefix = "INV-" + Year.now() + "-";
        long nextSeq = invoiceRepository.countByInvoiceNumberStartingWith(prefix) + 1;
        String invoiceNumber = prefix + String.format("%06d", nextSeq);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setPayment(payment);
        invoice.setCustomerUsernameSnapshot(payment.getBooking().getUser().getUsername());
        invoice.setRoomNameSnapshot(payment.getBooking().getRoom().getName());
        invoice.setAmount(payment.getAmount());
        invoice.setInvoiceDate(LocalDate.now());
        invoice.setCreatedAt(Instant.now());

        return invoiceRepository.save(invoice);
    }

    @Override
    public InvoiceResponseDTO getForBooking(Long bookingId) {
        Payment payment = paymentRepository.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + bookingId));
        authorizationService.requireOwnerOrAdmin(payment.getBooking().getUser().getId());

        Invoice invoice = invoiceRepository.findByPayment_Id(payment.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No invoice yet for booking: " + bookingId));

        return InvoiceResponseDTO.from(invoice);
    }
}
