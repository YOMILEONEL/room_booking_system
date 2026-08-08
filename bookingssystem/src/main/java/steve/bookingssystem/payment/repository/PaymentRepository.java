package steve.bookingssystem.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import steve.bookingssystem.payment.model.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBooking_BookingId(UUID bookingId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p "
            + "where p.status = steve.bookingssystem.payment.model.PaymentStatus.PAID "
            + "and p.paidAt between :start and :end")
    BigDecimal sumRevenueBetween(@Param("start") Instant start, @Param("end") Instant end);
}
