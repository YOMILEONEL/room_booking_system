package steve.bookingssystem.booking.model;

import org.junit.jupiter.api.Test;
import steve.bookingssystem.payment.model.Payment;
import steve.bookingssystem.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;

// Regression test for docs/code-review.md 1.8: Booking and Payment both had Lombok @Data with a
// mutual @OneToOne reference, so the generated toString()/equals()/hashCode() recursed into each
// other forever - every debug log of a booking with a payment, or putting one in a Set/HashMap,
// threw StackOverflowError.
class BookingPaymentCircularReferenceTest {

    private Booking bookingWithPayment() {
        Booking booking = new Booking();
        Payment payment = new Payment();
        payment.setAmount(new BigDecimal("100.00"));
        payment.setStatus(PaymentStatus.PENDING);
        payment.setBooking(booking);
        booking.setPayment(payment);
        return booking;
    }

    @Test
    void toString_doesNotRecurseForever() {
        Booking booking = bookingWithPayment();

        assertThatCode(booking::toString).doesNotThrowAnyException();
        assertThatCode(() -> booking.getPayment().toString()).doesNotThrowAnyException();
    }

    @Test
    void equalsAndHashCode_doNotRecurseForever() {
        Booking booking = bookingWithPayment();

        assertThatCode(() -> {
            Set<Booking> bookings = new HashSet<>();
            bookings.add(booking);
            bookings.contains(booking);

            Set<Payment> payments = new HashSet<>();
            payments.add(booking.getPayment());
            payments.contains(booking.getPayment());
        }).doesNotThrowAnyException();
    }
}
