package steve.bookingssystem.booking.Controller;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.model.BookingDTO;
import steve.bookingssystem.booking.model.BookingResponseDTO;
import steve.bookingssystem.booking.service.BookingService;

import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponseDTO saveBooking(@Valid @RequestBody BookingDTO booking) {
        return bookingService.addBooking(booking);
    }

    @GetMapping("/getAll/{userId}")
    public List<BookingResponseDTO> getBookings(@PathVariable Long userId) {
        return bookingService.getBookings(userId);
    }

    @PutMapping("/update/{id}/{userId}")
    public BookingResponseDTO updateBooking(@PathVariable Long id, @RequestBody Booking booking, @PathVariable Long userId) {
        return bookingService.updateBooking(id, booking, userId);
    }

    @DeleteMapping("/delete/{id}/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteBooking(@PathVariable Long id, @PathVariable Long userId) {
        bookingService.deleteBooking(id, userId);
    }

    @GetMapping("get/{id}/{userId}")
    public BookingResponseDTO getBooking(@PathVariable Long id, @PathVariable Long userId) {
        return bookingService.getBooking(id, userId);
    }

}
