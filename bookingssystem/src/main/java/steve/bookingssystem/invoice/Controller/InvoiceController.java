package steve.bookingssystem.invoice.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import steve.bookingssystem.invoice.model.InvoiceResponseDTO;
import steve.bookingssystem.invoice.service.InvoiceService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/invoice")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/booking/{bookingId}")
    public InvoiceResponseDTO getForBooking(@PathVariable Long bookingId) {
        return invoiceService.getForBooking(bookingId);
    }

}
