package steve.bookingssystem.booking.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import steve.bookingssystem.booking.model.AdminBookingDTO;
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
import steve.bookingssystem.user.model.CustomerType;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.model.UserRole;
import steve.bookingssystem.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

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
    public BookingResponseDTO updateBooking(UUID id, Booking newBooking, UUID userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        authorizationService.requireOwnerOrAdmin(booking.getUser().getId());

        UUID roomId = newBooking.getRoom() != null ? newBooking.getRoom().getId() : booking.getRoom().getId();
        assertNoOverlap(roomId, newBooking.getStartTime(), newBooking.getEndTime(), id);

        booking.setRoom(newBooking.getRoom());
        booking.setStartTime(newBooking.getStartTime());
        booking.setEndTime(newBooking.getEndTime());
        bookingRepository.save(booking);
        return BookingResponseDTO.from(booking);
    }

    @Override
    public void deleteBooking(UUID id, UUID userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        authorizationService.requireOwnerOrAdmin(booking.getUser().getId());

        if (booking.getPayment() != null && booking.getPayment().getStatus() == PaymentStatus.PAID) {
            throw new BookingConflictException("Bezahlte Buchungen können nicht gelöscht werden");
        }

        LocalDate today = LocalDate.now();
        if (!today.isBefore(booking.getStartTime()) && !today.isAfter(booking.getEndTime())) {
            throw new BookingConflictException("Laufende Buchungen können nicht gelöscht werden");
        }

        bookingRepository.delete(booking);
    }

    @Override
    public BookingResponseDTO getBooking(UUID id, UUID userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        authorizationService.requireOwnerOrAdmin(booking.getUser().getId());
        return BookingResponseDTO.from(booking);
    }

    @Override
    public List<BookingResponseDTO> getBookings(UUID userId) {
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
        // Admins manage the system, they don't personally consume it - self-booking (for
        // themselves or anyone else via this endpoint) is off-limits. The only sanctioned path
        // for an admin to create a booking is addBookingForCustomer, by customer email lookup.
        if (authorizationService.isAdmin()) {
            throw new AccessDeniedException("Admins können keine Räume über diesen Weg buchen");
        }
        authorizationService.requireOwnerOrAdmin(booking.getUserId());

        if (booking.getStartTime() == null || booking.getEndTime() == null
                || booking.getStartTime().isAfter(booking.getEndTime())) {
            throw new IllegalArgumentException("Enddatum darf nicht vor Startdatum liegen");
        }

        User user = userRepository.findById(booking.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + booking.getUserId()));

        return createBookingFor(user, booking.getRoomId(), booking.getStartTime(), booking.getEndTime(), booking.getDiscountCode());
    }

    @Override
    public BookingResponseDTO addBookingForCustomer(AdminBookingDTO booking) {
        authorizationService.requireAdmin();

        User customer = userRepository.findByUsername(booking.getCustomerEmail());
        if (customer == null) {
            throw new ResourceNotFoundException("Kunde nicht gefunden: " + booking.getCustomerEmail());
        }
        if (customer.getRole() != UserRole.MEMBER) {
            throw new IllegalArgumentException("Diese E-Mail gehört zu keinem Kunden-Konto");
        }

        return createBookingFor(customer, booking.getRoomId(), booking.getStartTime(), booking.getEndTime(), booking.getDiscountCode());
    }

    private BookingResponseDTO createBookingFor(User user, UUID roomId, LocalDate startTime, LocalDate endTime, String discountCode) {
        if (startTime == null || endTime == null || startTime.isAfter(endTime)) {
            throw new IllegalArgumentException("Enddatum darf nicht vor Startdatum liegen");
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));

        assertNoOverlap(room.getId(), startTime, endTime, null);

        Booking newBooking = new Booking();
        newBooking.setRoom(room);
        newBooking.setStartTime(startTime);
        newBooking.setEndTime(endTime);
        newBooking.setUser(user);
        newBooking.setCreatedAt(Instant.now());
        bookingRepository.save(newBooking);

        long days = ChronoUnit.DAYS.between(startTime, endTime) + 1;
        BigDecimal baseAmount = room.getPricePerDay().multiply(BigDecimal.valueOf(days));
        boolean isOrganisation = user.getCustomerType() == CustomerType.ORGANISATION;
        BigDecimal amount = user.getCustomerType() != null ? user.getCustomerType().applyPricing(baseAmount) : baseAmount;

        if (discountCode != null && !discountCode.isBlank()) {
            if (isOrganisation) {
                throw new IllegalArgumentException(
                        "Organisationen erhalten bereits reduzierte Preise und können keine Rabattcodes einlösen.");
            }
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

    private void assertNoOverlap(UUID roomId, LocalDate startTime, LocalDate endTime, UUID excludeBookingId) {
        List<Booking> overlapping = bookingRepository.findOverlapping(roomId, startTime, endTime, excludeBookingId);
        if (!overlapping.isEmpty()) {
            throw new BookingConflictException("Room is already booked for this timeframe");
        }
    }

}
