package steve.bookingssystem.booking.Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.service.BookingService;

import java.util.List;

@RestController
@RequestMapping("/booking")
public class BookingController {


    private BookingService bookingService;

    @PostMapping("/add/{roomId}")
    public ResponseEntity<Void> saveBooking(@RequestBody Booking booking, @PathVariable Long roomId) {
        return bookingService.addBooking(booking, roomId);
    }

    @GetMapping("get")
    public List<Booking> getBookings() {
        return bookingService.getBookings();
    }

    @PutMapping
    public ResponseEntity<Void> updateBooking(Long id, @RequestBody Booking booking) {
        return bookingService.updateBooking(id, booking);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        return bookingService.deleteBooking(id);
    }

    @GetMapping("get/{id}")
    public Booking getBooking(@PathVariable Long id) {
        return bookingService.getBooking(id);
    }

}
