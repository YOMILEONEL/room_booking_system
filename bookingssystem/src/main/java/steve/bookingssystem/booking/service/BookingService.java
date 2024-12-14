package steve.bookingssystem.booking.service;

import org.springframework.http.ResponseEntity;
import steve.bookingssystem.booking.model.Booking;

import java.util.List;
import java.util.Optional;

public interface BookingService {
    public ResponseEntity<Void> updateBooking(Long id, Booking booking);
    public ResponseEntity<Void> deleteBooking(Long id);
    public Booking getBooking(Long id);
    public List<Booking> getBookings();
    public ResponseEntity<Void> addBooking(Booking booking, Long roomId);
}
