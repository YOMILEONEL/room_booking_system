package steve.bookingssystem.payment.service;

import steve.bookingssystem.payment.model.Payment;
import steve.bookingssystem.payment.model.PaymentResponseDTO;

import java.util.UUID;

public interface PaymentService {
    PaymentResponseDTO getForBooking(UUID bookingId);

    // Admin-only technical override - see PaymentServiceImpl.confirmPayment.
    PaymentResponseDTO confirmPayment(UUID paymentId);

    // The actual "mark as paid" logic (status, paidAt, invoice generation), shared between the
    // override above and StripeServiceImpl's webhook handler - the real, Stripe-driven path.
    // No authorization check here on purpose: callers (an already-admin-checked confirmPayment,
    // or a signature-verified Stripe webhook) are each responsible for their own gate before
    // calling this.
    Payment markPaid(Payment payment);
}
