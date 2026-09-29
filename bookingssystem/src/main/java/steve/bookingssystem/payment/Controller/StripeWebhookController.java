package steve.bookingssystem.payment.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import steve.bookingssystem.payment.service.StripeService;

// Separate from PaymentController on purpose: everything else under /payment requires a Bearer
// token, this one endpoint deliberately doesn't (Stripe has no JWT to send) - security here comes
// entirely from StripeServiceImpl.handleWebhookEvent's signature check, not from Spring Security.
// See SecurityConfig's permitAll() list and docs/stripe-payments.md.
@RestController
@RequestMapping("/payment/stripe")
@Tag(name = "Stripe-Webhook", description = "Öffentlicher Endpunkt für Stripe-Zahlungsereignisse - Sicherheit über Signaturprüfung, nicht Bearer-Token")
@SecurityRequirements
public class StripeWebhookController {

    @Autowired
    private StripeService stripeService;

    @PostMapping("/webhook")
    @Operation(summary = "Stripe-Zahlungsereignis empfangen",
            description = "Wird von Stripe aufgerufen, nicht von der eigenen Oberfläche. Prüft die " +
                    "Stripe-Signature-Header-Signatur gegen STRIPE_WEBHOOK_SECRET, bevor irgendetwas mit " +
                    "dem Inhalt passiert - siehe docs/stripe-payments.md.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event verarbeitet (oder bewusst ignoriert, z. B. unbekannter Event-Typ)"),
            @ApiResponse(responseCode = "400", description = "Ungültige oder fehlende Signatur")
    })
    public void webhook(@RequestBody String payload,
                         @Parameter(description = "Von Stripe gesetzter Signatur-Header") @RequestHeader("Stripe-Signature") String signature) {
        stripeService.handleWebhookEvent(payload, signature);
    }

}
