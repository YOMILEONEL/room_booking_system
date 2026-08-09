package steve.bookingssystem.booking.service;

import steve.bookingssystem.booking.model.AdminBookingDTO;
import steve.bookingssystem.booking.model.BookingDTO;
import steve.bookingssystem.booking.model.BookingResponseDTO;

import java.util.List;
import java.util.UUID;

public interface BookingService {
    void deleteBooking(UUID id);
    BookingResponseDTO getBooking(UUID id);
    List<BookingResponseDTO> getBookings();
    BookingResponseDTO addBooking(BookingDTO booking);
    BookingResponseDTO addBookingForCustomer(AdminBookingDTO booking);
}
