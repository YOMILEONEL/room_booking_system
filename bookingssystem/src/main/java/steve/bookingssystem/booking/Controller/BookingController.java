package steve.bookingssystem.booking.Controller;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.booking.model.AdminBookingDTO;
import steve.bookingssystem.booking.model.BookingDTO;
import steve.bookingssystem.booking.model.BookingResponseDTO;
import steve.bookingssystem.booking.service.BookingService;

import java.util.List;
import java.util.UUID;

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

    @PostMapping("/admin-add")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponseDTO saveBookingForCustomer(@Valid @RequestBody AdminBookingDTO booking) {
        return bookingService.addBookingForCustomer(booking);
    }

    @GetMapping("/getAll")
    public List<BookingResponseDTO> getBookings() {
        return bookingService.getBookings();
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteBooking(@PathVariable UUID id) {
        bookingService.deleteBooking(id);
    }

    @GetMapping("get/{id}")
    public BookingResponseDTO getBooking(@PathVariable UUID id) {
        return bookingService.getBooking(id);
    }

}
