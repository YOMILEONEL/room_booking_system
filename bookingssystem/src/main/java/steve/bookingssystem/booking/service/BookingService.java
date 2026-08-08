package steve.bookingssystem.booking.service;

import steve.bookingssystem.booking.model.AdminBookingDTO;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.model.BookingDTO;
import steve.bookingssystem.booking.model.BookingResponseDTO;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    BookingResponseDTO updateBooking(UUID id, Booking booking, UUID userId);
    void deleteBooking(UUID id, UUID userId);
    BookingResponseDTO getBooking(UUID id, UUID userId);
    List<BookingResponseDTO> getBookings(UUID userId);
    BookingResponseDTO addBooking(BookingDTO booking);
    BookingResponseDTO addBookingForCustomer(AdminBookingDTO booking);
}
