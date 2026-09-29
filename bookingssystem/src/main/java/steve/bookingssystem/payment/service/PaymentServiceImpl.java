package steve.bookingssystem.payment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    // Technical override, not part of the normal flow anymore - payments are meant to reach PAID
    // only via Stripe (see StripeServiceImpl.handleWebhookEvent, which calls the same markPaid()
    // below). Kept for edge cases the payment provider can't cover on its own (e.g. cash paid in
    // person) and deliberately not exposed in the admin UI anymore - see docs/stripe-payments.md.
    @Override
    @Transactional
    public PaymentResponseDTO confirmPayment(UUID paymentId) {
        authorizationService.requireAdmin();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        return PaymentResponseDTO.from(markPaid(payment));
    }

    // Shared by confirmPayment (admin override) and StripeServiceImpl (the real, Stripe-driven
    // path). @Transactional so a failure while generating the invoice (e.g. an invoice-number
    // collision, see InvoiceServiceImpl) rolls the PAID status back too, instead of leaving a
    // payment marked PAID that never gets an invoice.
    @Override
    @Transactional
    public Payment markPaid(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Payment is already marked as paid");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(Instant.now());
        paymentRepository.save(payment);
        invoiceService.generateForPayment(payment);
        return payment;
    }
}
