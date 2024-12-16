package steve.bookingssystem.booking.Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.model.BookingDTO;
import steve.bookingssystem.booking.service.BookingService;

import java.util.List;

@RestController
@RequestMapping("/booking")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    @PostMapping("/add")
    public ResponseEntity<?> saveBooking(@RequestBody BookingDTO booking) {
        return bookingService.addBooking(booking);
    }

    @GetMapping("getAll/{userId}")
    public List<Booking> getBookings(@PathVariable Long userId) {
        return bookingService.getBookings(userId);
    }

    @PutMapping("/update/{id}/{userId}")
    public ResponseEntity<?> updateBooking(Long id, @RequestBody Booking booking, @PathVariable Long userId) {
        return bookingService.updateBooking(id, booking, userId);
    }

    @DeleteMapping("/delete/{id}/{userId}")
    public ResponseEntity<?> deleteBooking(@PathVariable Long id,@PathVariable Long userId) {
        return bookingService.deleteBooking(id, userId);
    }

    @GetMapping("get/{id}/{userId}")
    public Booking getBooking(@PathVariable Long id, @PathVariable Long userId) {
        return bookingService.getBooking(id, userId);
    }

}
