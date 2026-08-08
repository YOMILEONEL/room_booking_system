package steve.bookingssystem.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.invoice.service.InvoiceService;
import steve.bookingssystem.payment.model.Payment;
import steve.bookingssystem.payment.model.PaymentResponseDTO;
import steve.bookingssystem.payment.model.PaymentStatus;
import steve.bookingssystem.payment.repository.PaymentRepository;
import steve.bookingssystem.security.AuthorizationService;

import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private InvoiceService invoiceService;

    @Override
    public PaymentResponseDTO getForBooking(UUID bookingId) {
        Payment payment = paymentRepository.findByBooking_BookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for booking: " + bookingId));
        authorizationService.requireOwnerOrAdmin(payment.getBooking().getUser().getId());
        return PaymentResponseDTO.from(payment);
    }

    @Override
    public PaymentResponseDTO confirmPayment(UUID paymentId) {
        authorizationService.requireAdmin();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Payment is already marked as paid");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        invoiceService.generateForPayment(payment);
        return PaymentResponseDTO.from(payment);
    }
}
