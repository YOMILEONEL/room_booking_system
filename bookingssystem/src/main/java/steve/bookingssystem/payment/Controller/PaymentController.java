package steve.bookingssystem.payment.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.payment.model.PaymentResponseDTO;
import steve.bookingssystem.payment.service.PaymentService;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/booking/{bookingId}")
    public PaymentResponseDTO getForBooking(@PathVariable Long bookingId) {
        return paymentService.getForBooking(bookingId);
    }

    @PutMapping("/{id}/confirm")
    public PaymentResponseDTO confirmPayment(@PathVariable Long id) {
        return paymentService.confirmPayment(id);
    }

}
