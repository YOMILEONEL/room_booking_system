package steve.bookingssystem.invoice.Controller;

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
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/booking/{bookingId}")
    public InvoiceResponseDTO getForBooking(@PathVariable UUID bookingId) {
        return invoiceService.getForBooking(bookingId);
    }

    @GetMapping("/booking/{bookingId}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID bookingId) {
        InvoicePdfFile file = invoiceService.generatePdfForBooking(bookingId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.filename() + "\"")
                .body(file.content());
    }

}
