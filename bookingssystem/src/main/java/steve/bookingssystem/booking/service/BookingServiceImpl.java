package steve.bookingssystem.booking.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.repository.BookingRepository;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.model.Status;
import steve.bookingssystem.room.repository.RoomRepository;

import java.util.List;
import java.util.Optional;

public class BookingServiceImpl implements BookingService {

    RoomRepository roomRepository;
    BookingRepository bookingRepository;

    @Override
    public ResponseEntity<Void> updateBooking(Long id, Booking newBooking) {
        Optional<Booking> booking = bookingRepository.findById(id);

        if (booking.isPresent()) {
            Booking bookingExist = booking.get();
            bookingExist.setRoom(newBooking.getRoom());
            bookingExist.setStartTime(newBooking.getStartTime());
            bookingExist.setEndTime(newBooking.getEndTime());
            bookingRepository.save(bookingExist);
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Override
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        Optional<Booking> booking = bookingRepository.findById(id);

        if (booking.isPresent()) {
            roomRepository.findById(booking.get().getRoom().getId()).get().setRoomStatus(Status.VERFUGBAR);
            bookingRepository.delete(booking.get());
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    public Booking getBooking(@PathVariable Long id) {
        Optional<Booking> booking = bookingRepository.findById(id);
        if (booking.isPresent()) {
            return booking.get();
        }
        return null;
    }

    @Override
    public List<Booking> getBookings() {
        return bookingRepository.findAll();
    }

    @Override
    public ResponseEntity<Void> addBooking(@RequestBody Booking booking, @PathVariable Long roomId){
        Optional<Room> room = roomRepository.findById(roomId);

        if(room.isPresent()){
            booking.setRoom(room.get());
            bookingRepository.save(booking);
            roomRepository.findById(roomId).get().setRoomStatus(Status.GEBUCHT);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

}
