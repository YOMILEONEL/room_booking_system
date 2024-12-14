package steve.bookingssystem.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import steve.bookingssystem.booking.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {
}
