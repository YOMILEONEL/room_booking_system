package steve.bookingssystem.payment.service;

import steve.bookingssystem.payment.model.PaymentResponseDTO;

public interface PaymentService {
    PaymentResponseDTO getForBooking(Long bookingId);
    PaymentResponseDTO confirmPayment(Long paymentId);
}
