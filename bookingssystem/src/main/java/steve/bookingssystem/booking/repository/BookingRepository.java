package steve.bookingssystem.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import steve.bookingssystem.booking.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
}
