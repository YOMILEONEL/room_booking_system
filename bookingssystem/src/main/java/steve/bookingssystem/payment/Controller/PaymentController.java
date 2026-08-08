package steve.bookingssystem.payment.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.payment.model.PaymentResponseDTO;
import steve.bookingssystem.payment.service.PaymentService;

import java.util.UUID;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/booking/{bookingId}")
    public PaymentResponseDTO getForBooking(@PathVariable UUID bookingId) {
        return paymentService.getForBooking(bookingId);
    }

    @PutMapping("/{id}/confirm")
    public PaymentResponseDTO confirmPayment(@PathVariable UUID id) {
        return paymentService.confirmPayment(id);
    }

}
