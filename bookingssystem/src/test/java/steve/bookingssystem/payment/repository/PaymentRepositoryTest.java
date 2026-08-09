package steve.bookingssystem.payment.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.payment.model.Payment;
import steve.bookingssystem.payment.model.PaymentStatus;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserRole;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// Regression test for docs/code-review.md 1.12: sumRevenueBetween used a JPQL "between" (inclusive
// on both ends), while the caller (AdminDashboardServiceImpl) passes the *start of the next month*
// as the upper bound - a payment paid at exactly that instant used to count toward both months.
@DataJpaTest
@ActiveProfiles("test")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private steve.bookingssystem.booking.repository.BookingRepository bookingRepository;
    @Autowired
    private steve.bookingssystem.room.repository.RoomRepository roomRepository;
    @Autowired
    private steve.bookingssystem.user.repository.UserRepository userRepository;

    private Payment paidPaymentAt(Instant paidAt) {
        User user = new User();
        user.setEmail("test-" + paidAt.toEpochMilli() + "@example.com");
        user.setPassword("irrelevant-hash");
        user.setRole(UserRole.MEMBER);
        userRepository.save(user);

        Room room = new Room();
        room.setName("Konferenzraum");
        room.setCapacity(8);
        room.setLocation("EG");
        room.setPricePerDay(new BigDecimal("100.00"));
        roomRepository.save(room);

        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setUser(user);
        booking.setStartTime(LocalDate.now());
        booking.setEndTime(LocalDate.now().plusDays(1));
        booking.setCreatedAt(Instant.now());

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(paidAt);
        payment.setCreatedAt(Instant.now());
        booking.setPayment(payment);

        bookingRepository.save(booking);
        return paymentRepository.save(payment);
    }

    @Test
    void sumRevenueBetween_doesNotDoubleCountAPaymentPaidExactlyOnTheMonthBoundary() {
        Instant septemberFirstMidnight = LocalDate.of(2026, 9, 1)
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        paidPaymentAt(septemberFirstMidnight);

        Instant augustStart = LocalDate.of(2026, 8, 1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant septemberStart = septemberFirstMidnight;
        Instant octoberStart = LocalDate.of(2026, 10, 1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        BigDecimal augustRevenue = paymentRepository.sumRevenueBetween(augustStart, septemberStart);
        BigDecimal septemberRevenue = paymentRepository.sumRevenueBetween(septemberStart, octoberStart);

        assertThat(augustRevenue).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(septemberRevenue).isEqualByComparingTo("100.00");
    }
}
