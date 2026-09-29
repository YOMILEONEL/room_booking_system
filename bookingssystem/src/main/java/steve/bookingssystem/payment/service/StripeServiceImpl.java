package steve.bookingssystem.payment.service;

import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.payment.model.Payment;
import steve.bookingssystem.payment.model.PaymentStatus;
import steve.bookingssystem.payment.repository.PaymentRepository;
import steve.bookingssystem.security.AuthorizationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class StripeServiceImpl implements StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeServiceImpl.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private AuthorizationService authorizationService;
    // Reused rather than duplicated: PaymentServiceImpl.markPaid already does status/paidAt/
    // invoice generation atomically (@Transactional) - see PaymentServiceImpl.confirmPayment for
    // the (now admin-only-override) other caller of the same method.
    @Autowired
    private PaymentService paymentService;

    @Value("${stripe.secret-key}")
    private String secretKey;
    @Value("${stripe.webhook-secret}")
    private String webhookSecret;
    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public String createCheckoutSessionUrl(UUID paymentId) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("Stripe ist auf diesem Server nicht konfiguriert");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        authorizationService.requireOwnerOrAdmin(payment.getBooking().getUser().getId());

        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Payment is already marked as paid");
        }

        // amount is already stored with scale 2 (DB column precision=10, scale=2) - setScale
        // here is just defensive, then shift two places for Stripe's "smallest currency unit"
        // (cents) convention. longValueExact() is safe: the fractional part is always zero after
        // this multiplication for a 2-decimal amount.
        long amountInCents = payment.getAmount()
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        String roomName = payment.getBooking().getRoom().getName();
        String period = DATE_FORMAT.format(payment.getBooking().getStartTime())
                + " – " + DATE_FORMAT.format(payment.getBooking().getEndTime());

        try {
            StripeClient client = new StripeClient(secretKey);
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/profile?payment=success")
                    .setCancelUrl(frontendUrl + "/profile?payment=cancelled")
                    // Read back in the webhook (handleWebhookEvent below) to find the matching
                    // Payment row - the only correlation the webhook actually relies on.
                    .setClientReferenceId(payment.getId().toString())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("eur")
                                    .setUnitAmount(amountInCents)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(roomName + ", " + period)
                                            .build())
                                    .build())
                            .build())
                    .build();

            Session session = client.v1().checkout().sessions().create(params);

            // Traceability/idempotency only - not what the webhook uses to find this payment
            // (that's client_reference_id above), so a save failure here would still leave a
            // working checkout, just without this audit trail.
            payment.setStripeCheckoutSessionId(session.getId());
            paymentRepository.save(payment);

            return session.getUrl();
        } catch (StripeException e) {
            throw new IllegalStateException("Stripe-Checkout konnte nicht gestartet werden", e);
        }
    }

    @Override
    public void handleWebhookEvent(String payload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe-Webhook ist auf diesem Server nicht konfiguriert");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            // The one thing standing between "public, unauthenticated endpoint" and "anyone can
            // mark any booking as paid for free" - see docs/stripe-payments.md.
            throw new IllegalArgumentException("Invalid Stripe webhook signature", e);
        }

        // Card payments (the only method this integration offers, see docs/stripe-payments.md)
        // complete synchronously, so this one event type covers the whole flow. Every other
        // event Stripe might send is deliberately ignored, not an error.
        if (!"checkout.session.completed".equals(event.getType())) {
            return;
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty()) {
            return;
        }
        if (!(deserializer.getObject().get() instanceof Session session)) {
            return;
        }

        String clientReferenceId = session.getClientReferenceId();
        if (clientReferenceId == null) {
            log.warn("[stripe] checkout.session.completed event {} has no client_reference_id, ignoring", event.getId());
            return;
        }

        UUID paymentId;
        try {
            paymentId = UUID.fromString(clientReferenceId);
        } catch (IllegalArgumentException e) {
            log.warn("[stripe] checkout.session.completed event {} has a non-UUID client_reference_id '{}', ignoring",
                    event.getId(), clientReferenceId);
            return;
        }

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null) {
            log.warn("[stripe] checkout.session.completed event {} references unknown payment {}", event.getId(), paymentId);
            return;
        }

        // Stripe delivers webhooks at-least-once - a duplicate delivery for an already-confirmed
        // payment is expected, not an error, so this stays at DEBUG rather than INFO/WARN.
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.debug("[stripe] payment {} already PAID, ignoring duplicate webhook delivery (event {})", paymentId, event.getId());
            return;
        }

        paymentService.markPaid(payment);
        // The one log line that actually matters for debugging "did the webhook work?" locally -
        // everything above this point in the method only logs when something looked off.
        log.info("[stripe] payment {} marked PAID via webhook (event {})", paymentId, event.getId());
    }
}
