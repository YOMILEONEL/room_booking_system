package steve.bookingssystem.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.model.BookingDTO;
import steve.bookingssystem.booking.model.BookingResponseDTO;
import steve.bookingssystem.booking.repository.BookingRepository;
import steve.bookingssystem.discount.service.DiscountCodeService;
import steve.bookingssystem.exception.BookingConflictException;
import steve.bookingssystem.payment.model.Payment;
import steve.bookingssystem.payment.model.PaymentStatus;
import steve.bookingssystem.payment.repository.PaymentRepository;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.repository.RoomRepository;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.user.model.User;
import steve.bookingssystem.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final UUID ROOM_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private DiscountCodeService discountCodeService;
    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Room room(BigDecimal pricePerNight) {
        Room room = new Room();
        room.setId(ROOM_ID);
        room.setName("Konferenzraum");
        room.setCapacity(10);
        room.setLocation("EG");
        room.setPricePerNight(pricePerNight);
        return room;
    }

    private User user() {
        User user = new User();
        user.setId(USER_ID);
        user.setUsername("alice");
        user.setPassword("irrelevant-hash");
        return user;
    }

    private BookingDTO dto(LocalDate start, LocalDate end, String discountCode) {
        BookingDTO dto = new BookingDTO();
        dto.setRoomId(ROOM_ID);
        dto.setUserId(USER_ID);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setDiscountCode(discountCode);
        return dto;
    }

    @Test
    void addBooking_computesAmountFromPricePerNightAndNights() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(new BigDecimal("100.00"))));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(bookingRepository.findOverlapping(eq(ROOM_ID), any(), any(), eq(null))).thenReturn(List.of());

        BookingResponseDTO result = bookingService.addBooking(
                dto(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3), null));

        // 1st-3rd inclusive = 3 nights
        assertThat(result.getPayment().getAmount()).isEqualByComparingTo("300.00");
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.PENDING);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    void addBooking_rejectsOverlappingBooking() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(new BigDecimal("100.00"))));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(bookingRepository.findOverlapping(eq(ROOM_ID), any(), any(), eq(null)))
                .thenReturn(List.of(new steve.bookingssystem.booking.model.Booking()));

        assertThatThrownBy(() -> bookingService.addBooking(
                dto(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3), null)))
                .isInstanceOf(BookingConflictException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void addBooking_rejectsEndBeforeStart() {
        assertThatThrownBy(() -> bookingService.addBooking(
                dto(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 1), null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(roomRepository, never()).findById(any(UUID.class));
    }

    @Test
    void addBooking_appliesDiscountCodeToAmount() {
        when(roomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room(new BigDecimal("100.00"))));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
        when(bookingRepository.findOverlapping(eq(ROOM_ID), any(), any(), eq(null))).thenReturn(List.of());
        when(discountCodeService.applyDiscount(eq("SUMMER10"), any(BigDecimal.class)))
                .thenReturn(new BigDecimal("270.00"));

        BookingResponseDTO result = bookingService.addBooking(
                dto(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3), "SUMMER10"));

        assertThat(result.getPayment().getAmount()).isEqualByComparingTo("270.00");
        assertThat(result.getPayment().getAppliedDiscountCode()).isEqualTo("SUMMER10");
    }

    @Test
    void addBooking_rejectsWhenCallerIsNeitherOwnerNorAdmin() {
        doThrow(new AccessDeniedException("Not allowed"))
                .when(authorizationService).requireOwnerOrAdmin(USER_ID);

        assertThatThrownBy(() -> bookingService.addBooking(
                dto(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 3), null)))
                .isInstanceOf(AccessDeniedException.class);

        verify(roomRepository, never()).findById(any(UUID.class));
    }

    private Booking booking(LocalDate start, LocalDate end, PaymentStatus paymentStatus) {
        Booking booking = new Booking();
        booking.setBookingId(UUID.randomUUID());
        booking.setUser(user());
        booking.setStartTime(start);
        booking.setEndTime(end);
        if (paymentStatus != null) {
            Payment payment = new Payment();
            payment.setStatus(paymentStatus);
            booking.setPayment(payment);
        }
        return booking;
    }

    @Test
    void deleteBooking_rejectsWhenPaymentIsPaid() {
        Booking booking = booking(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 3), PaymentStatus.PAID);
        when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.deleteBooking(booking.getBookingId(), USER_ID))
                .isInstanceOf(BookingConflictException.class);

        verify(bookingRepository, never()).delete(any());
    }

    @Test
    void deleteBooking_rejectsWhenBookingIsCurrentlyRunning() {
        LocalDate today = LocalDate.now();
        Booking booking = booking(today.minusDays(1), today.plusDays(1), PaymentStatus.PENDING);
        when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.deleteBooking(booking.getBookingId(), USER_ID))
                .isInstanceOf(BookingConflictException.class);

        verify(bookingRepository, never()).delete(any());
    }

    @Test
    void deleteBooking_deletesWhenUnpaidAndNotRunning() {
        Booking booking = booking(LocalDate.of(2099, 1, 1), LocalDate.of(2099, 1, 3), PaymentStatus.PENDING);
        when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));

        bookingService.deleteBooking(booking.getBookingId(), USER_ID);

        verify(bookingRepository).delete(booking);
    }
}
