package steve.bookingssystem.payment.service;

import steve.bookingssystem.payment.model.PaymentResponseDTO;

import java.util.UUID;

public interface PaymentService {
    PaymentResponseDTO getForBooking(UUID bookingId);
    PaymentResponseDTO confirmPayment(UUID paymentId);
}
