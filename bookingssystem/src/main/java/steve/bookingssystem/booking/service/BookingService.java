package steve.bookingssystem.booking.service;

import org.springframework.http.ResponseEntity;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.model.BookingDTO;

import java.util.List;
import java.util.Optional;

public interface BookingService {
    public ResponseEntity<?> updateBooking(Long id, Booking booking, Long userId);
    public ResponseEntity<?> deleteBooking(Long id, Long userId);
    public Booking getBooking(Long id, Long userId);
    public List<Booking> getBookings(Long userId);
    public ResponseEntity<?> addBooking(BookingDTO booking);
}
