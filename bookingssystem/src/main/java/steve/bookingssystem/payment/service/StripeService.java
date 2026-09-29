package steve.bookingssystem.payment.service;

import java.util.UUID;

public interface StripeService {
    // Returns the URL of a Stripe-hosted Checkout page the caller should redirect the browser
    // to. Throws IllegalStateException (-> 409, see GlobalExceptionHandler) if the payment is
    // already PAID.
    String createCheckoutSessionUrl(UUID paymentId);

    // Verifies the Stripe-Signature header against the raw request body, then - only for
    // "checkout.session.completed" events - marks the matching Payment as paid. Throws on an
    // invalid/missing signature; everything else (unrelated event types, an already-paid
    // payment) is a deliberate no-op, not an error, since Stripe treats a non-2xx response as
    // "retry this webhook".
    void handleWebhookEvent(String payload, String signatureHeader);
}
