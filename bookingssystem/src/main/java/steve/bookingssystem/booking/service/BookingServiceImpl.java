package steve.bookingssystem.booking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.model.BookingDTO;
import steve.bookingssystem.booking.model.BookingResponseDTO;
import steve.bookingssystem.booking.repository.BookingRepository;
import steve.bookingssystem.discount.service.DiscountCodeService;
import steve.bookingssystem.exception.BookingConflictException;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.payment.model.Payment;
import steve.bookingssystem.payment.model.PaymentStatus;
import steve.bookingssystem.payment.repository.PaymentRepository;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.repository.RoomRepository;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    @Autowired
    RoomRepository roomRepository;
    @Autowired
    BookingRepository bookingRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PaymentRepository paymentRepository;
    @Autowired
    DiscountCodeService discountCodeService;
    @Autowired
    AuthorizationService authorizationService;

    @Override
    public BookingResponseDTO updateBooking(Long id, Booking newBooking, Long userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        authorizationService.requireOwnerOrAdmin(booking.getUser().getId());

        Long roomId = newBooking.getRoom() != null ? newBooking.getRoom().getId() : booking.getRoom().getId();
        assertNoOverlap(roomId, newBooking.getStartTime(), newBooking.getEndTime(), id);

        booking.setRoom(newBooking.getRoom());
        booking.setStartTime(newBooking.getStartTime());
        booking.setEndTime(newBooking.getEndTime());
        bookingRepository.save(booking);
        return BookingResponseDTO.from(booking);
    }

    @Override
    public void deleteBooking(Long id, Long userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        authorizationService.requireOwnerOrAdmin(booking.getUser().getId());
        bookingRepository.delete(booking);
    }

    @Override
    public BookingResponseDTO getBooking(Long id, Long userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        authorizationService.requireOwnerOrAdmin(booking.getUser().getId());
        return BookingResponseDTO.from(booking);
    }

    @Override
    public List<BookingResponseDTO> getBookings(Long userId) {
        List<Booking> bookings;
        if (authorizationService.isAdmin()) {
            bookings = bookingRepository.findAll();
        } else {
            bookings = bookingRepository.findByUser_Id(authorizationService.requireAuthenticatedUserId());
        }
        return bookings.stream().map(BookingResponseDTO::from).toList();
    }

    @Override
    public BookingResponseDTO addBooking(BookingDTO booking) {
        authorizationService.requireOwnerOrAdmin(booking.getUserId());

        if (booking.getStartTime() == null || booking.getEndTime() == null
                || booking.getStartTime().isAfter(booking.getEndTime())) {
            throw new IllegalArgumentException("Enddatum darf nicht vor Startdatum liegen");
        }

        Room room = roomRepository.findById(booking.getRoomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + booking.getRoomId()));
        User user = userRepository.findById(booking.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + booking.getUserId()));

        assertNoOverlap(room.getId(), booking.getStartTime(), booking.getEndTime(), null);

        Booking newBooking = new Booking();
        newBooking.setRoom(room);
        newBooking.setStartTime(booking.getStartTime());
        newBooking.setEndTime(booking.getEndTime());
        newBooking.setUser(user);
        bookingRepository.save(newBooking);

        long nights = ChronoUnit.DAYS.between(booking.getStartTime(), booking.getEndTime()) + 1;
        BigDecimal amount = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        String discountCode = booking.getDiscountCode();
        if (discountCode != null && !discountCode.isBlank()) {
            amount = discountCodeService.applyDiscount(discountCode, amount);
        }

        Payment payment = new Payment();
        payment.setBooking(newBooking);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAppliedDiscountCode(discountCode != null && !discountCode.isBlank() ? discountCode : null);
        payment.setCreatedAt(Instant.now());
        paymentRepository.save(payment);
        newBooking.setPayment(payment);

        return BookingResponseDTO.from(newBooking);
    }

    private void assertNoOverlap(Long roomId, LocalDate startTime, LocalDate endTime, Long excludeBookingId) {
        List<Booking> overlapping = bookingRepository.findOverlapping(roomId, startTime, endTime, excludeBookingId);
        if (!overlapping.isEmpty()) {
            throw new BookingConflictException("Room is already booked for this timeframe");
        }
    }

}
