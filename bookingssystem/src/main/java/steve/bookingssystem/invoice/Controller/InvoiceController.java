package steve.bookingssystem.invoice.Controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import steve.bookingssystem.invoice.model.InvoicePdfFile;
import steve.bookingssystem.invoice.model.InvoiceResponseDTO;
import steve.bookingssystem.invoice.service.InvoiceService;

import java.util.UUID;

@RestController
@RequestMapping("/invoice")
@Tag(name = "Rechnungen", description = "Rechnung je bezahlter Buchung (Besitzer/Admin)")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Rechnungsdaten zu einer Buchung abrufen",
            description = "Nur vorhanden, sobald ein Admin die Zahlung der Buchung bestätigt hat.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rechnungsdaten geliefert"),
            @ApiResponse(responseCode = "403", description = "Fremde Buchung, kein Admin"),
            @ApiResponse(responseCode = "404", description = "Buchung oder Rechnung nicht gefunden")
    })
    public InvoiceResponseDTO getForBooking(@Parameter(description = "bookingId der Buchung") @PathVariable UUID bookingId) {
        return invoiceService.getForBooking(bookingId);
    }

    @GetMapping("/booking/{bookingId}/pdf")
    @Operation(summary = "Rechnung als PDF herunterladen")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF-Datei",
                    content = @Content(mediaType = MediaType.APPLICATION_PDF_VALUE, schema = @Schema(type = "string", format = "binary"))),
            @ApiResponse(responseCode = "403", description = "Fremde Buchung, kein Admin"),
            @ApiResponse(responseCode = "404", description = "Buchung oder Rechnung nicht gefunden")
    })
    public ResponseEntity<byte[]> downloadPdf(@Parameter(description = "bookingId der Buchung") @PathVariable UUID bookingId) {
        InvoicePdfFile file = invoiceService.generatePdfForBooking(bookingId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .body(file.content());
    }

}
