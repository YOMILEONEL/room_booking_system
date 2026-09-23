package steve.bookingssystem.payment.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.payment.model.PaymentResponseDTO;
import steve.bookingssystem.payment.service.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/payment")
@Tag(name = "Zahlungen", description = "Zahlung je Buchung (offen/bezahlt) - Bestätigung nur durch Admin")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Zahlung zu einer Buchung abrufen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zahlung geliefert"),
            @ApiResponse(responseCode = "403", description = "Fremde Buchung, kein Admin"),
            @ApiResponse(responseCode = "404", description = "Buchung oder Zahlung nicht gefunden")
    })
    public PaymentResponseDTO getForBooking(@Parameter(description = "bookingId der Buchung") @PathVariable UUID bookingId) {
        return paymentService.getForBooking(bookingId);
    }

    @PutMapping("/{id}/confirm")
    @Operation(summary = "Zahlung bestätigen (nur Admin)",
            description = "Setzt die Zahlung auf PAID und erzeugt dabei automatisch eine Rechnung.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Zahlung bestätigt, Rechnung erzeugt"),
            @ApiResponse(responseCode = "403", description = "Kein Admin-Konto"),
            @ApiResponse(responseCode = "404", description = "Zahlung nicht gefunden"),
            @ApiResponse(responseCode = "409", description = "Zahlung bereits bestätigt")
    })
    public PaymentResponseDTO confirmPayment(@Parameter(description = "ID der Zahlung") @PathVariable UUID id) {
        return paymentService.confirmPayment(id);
    }

}
