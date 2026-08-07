package steve.bookingssystem.booking.service;

import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.model.BookingDTO;
import steve.bookingssystem.booking.model.BookingResponseDTO;

import java.util.List;

public interface BookingService {
    BookingResponseDTO updateBooking(Long id, Booking booking, Long userId);
    void deleteBooking(Long id, Long userId);
    BookingResponseDTO getBooking(Long id, Long userId);
    List<BookingResponseDTO> getBookings(Long userId);
    BookingResponseDTO addBooking(BookingDTO booking);
}
