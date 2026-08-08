package steve.bookingssystem.payment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.invoice.service.InvoiceService;
import steve.bookingssystem.payment.model.Payment;
import steve.bookingssystem.payment.model.PaymentStatus;
import steve.bookingssystem.payment.repository.PaymentRepository;
import steve.bookingssystem.security.AuthorizationService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    private static final UUID PAYMENT_ID = UUID.randomUUID();
    private static final UUID BOOKING_ID = UUID.randomUUID();

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment pendingPayment() {
        Booking booking = new Booking();
        booking.setBookingId(BOOKING_ID);

        Payment payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setBooking(booking);
        payment.setAmount(new BigDecimal("150.00"));
        payment.setStatus(PaymentStatus.PENDING);
        return payment;
    }

    @Test
    void confirmPayment_marksPaidAndGeneratesInvoice() {
        Payment payment = pendingPayment();
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        paymentService.confirmPayment(PAYMENT_ID);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getPaidAt()).isNotNull();
        verify(paymentRepository).save(payment);
        verify(invoiceService).generateForPayment(payment);
    }

    @Test
    void confirmPayment_throwsWhenAlreadyPaid() {
        Payment payment = pendingPayment();
        payment.setStatus(PaymentStatus.PAID);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.confirmPayment(PAYMENT_ID))
                .isInstanceOf(IllegalStateException.class);

        verify(invoiceService, never()).generateForPayment(any());
    }

    @Test
    void confirmPayment_requiresAdmin() {
        doThrow(new AccessDeniedException("Admin only")).when(authorizationService).requireAdmin();

        assertThatThrownBy(() -> paymentService.confirmPayment(PAYMENT_ID))
                .isInstanceOf(AccessDeniedException.class);

        verify(paymentRepository, never()).findById(any());
    }
}
