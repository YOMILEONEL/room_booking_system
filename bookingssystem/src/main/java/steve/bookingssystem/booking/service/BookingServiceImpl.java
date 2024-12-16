package steve.bookingssystem.booking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.model.BookingDTO;
import steve.bookingssystem.booking.repository.BookingRepository;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.model.Status;
import steve.bookingssystem.room.repository.RoomRepository;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserRole;
import steve.bookingssystem.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    RoomRepository roomRepository;
    @Autowired
    BookingRepository bookingRepository;
    @Autowired
    UserRepository userRepository;

    @Override
    public ResponseEntity<?> updateBooking(Long id, Booking newBooking, Long userId) {
        Optional<Booking> booking = bookingRepository.findById(id);
        Optional<User> user = userRepository.findById(userId);

        if (booking.isPresent()) {
            if(user.get().getRole()== UserRole.ADMIN) {
                Booking bookingExist = booking.get();
                bookingExist.setRoom(newBooking.getRoom());
                bookingExist.setStartTime(newBooking.getStartTime());
                bookingExist.setEndTime(newBooking.getEndTime());
                bookingRepository.save(bookingExist);
                return ResponseEntity.status(HttpStatus.OK).build();
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not allowed to update this booking");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @Override
    public ResponseEntity<?> deleteBooking(@PathVariable Long id, Long userId) {
        Optional<Booking> booking = bookingRepository.findById(id);
        Optional<User> user = userRepository.findById(userId);

        if (booking.isPresent()) {
            if(user.get().getRole()== UserRole.ADMIN) {
                roomRepository.findById(booking.get().getRoom().getId()).get().setRoomStatus(Status.VERFUGBAR);
                bookingRepository.delete(booking.get());
                return new ResponseEntity<>(HttpStatus.OK);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You are not allowed to delete this booking");
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Override
    public Booking getBooking(@PathVariable Long id, Long userId) {
        Optional<Booking> booking = bookingRepository.findById(id);
        Optional<User> user = userRepository.findById(userId);
        if (booking.isPresent()) {
            if(user.isPresent()){
                if(user.get().getRole()== UserRole.ADMIN) {
                    return booking.get();
                }
                if(booking.get().getUser().getId() == userId){
                    return booking.get();
                }
            }

        }
        return null;
    }

    @Override
    public List<Booking> getBookings(Long userId) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            if(user.get().getRole()== UserRole.ADMIN) {
                return bookingRepository.findAll();
            }
            return null;
        }
        return null;
    }

    @Override
    public ResponseEntity<?> addBooking(BookingDTO booking){
        Optional<Room> room = roomRepository.findById(booking.getRoomId());
        Optional<User> user = userRepository.findById(booking.getUserId());
        System.out.println(1);

        if(room.isPresent()){
            System.out.println(2);
            if(user.isPresent()){
                System.out.println(3);
                if(user.get().getRole()== UserRole.ADMIN){

                    System.out.println(4);

                    Booking booking1 = new Booking();

                    booking1.setRoom(room.get());
                    booking1.setStartTime(booking.getStartTime());
                    booking1.setEndTime(booking.getEndTime());
                    booking1.setUser(user.get());

                    bookingRepository.save(booking1);

                    Room roomExisting = room.get();
                    roomExisting.setRoomStatus(Status.GEBUCHT);
                    roomRepository.save(roomExisting);

                    return ResponseEntity.status(HttpStatus.CREATED).build();
                }
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not allowed to add this booking");
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

}
