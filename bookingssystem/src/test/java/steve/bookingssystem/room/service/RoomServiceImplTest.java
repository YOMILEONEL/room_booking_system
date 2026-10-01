package steve.bookingssystem.room.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.repository.BookingRepository;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.room.model.BookedPeriodDTO;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.model.RoomResponseDTO;
import steve.bookingssystem.room.model.Status;
import steve.bookingssystem.room.repository.RoomRepository;
import steve.bookingssystem.security.AuthorizationService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private RoomRepository roomRepository;
    @Mock
    private AuthorizationService authorizationService;
    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private RoomServiceImpl roomService;

    private Room room(UUID id, boolean active) {
        Room room = new Room();
        room.setId(id);
        room.setName("Konferenzraum");
        room.setCapacity(8);
        room.setLocation("EG");
        room.setPricePerDay(new BigDecimal("100.00"));
        room.setActive(active);
        return room;
    }

    private Booking booking(Room room, LocalDate end) {
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setStartTime(LocalDate.now());
        booking.setEndTime(end);
        return booking;
    }

    @Test
    void findAllRooms_survivesTwoActiveBookingsForTheSameRoom() {
        UUID roomId = UUID.randomUUID();
        Room room = room(roomId, true);
        lenient().when(authorizationService.isAdmin()).thenReturn(false);
        when(roomRepository.findByActiveTrue()).thenReturn(List.of(room));
        // Two active bookings for the same room today - this used to throw
        // IllegalStateException("Duplicate key ...") from Collectors.toMap and take down the
        // whole room list (see docs/code-review.md, 1.7).
        when(bookingRepository.findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(any(), any()))
                .thenReturn(List.of(
                        booking(room, LocalDate.now().plusDays(1)),
                        booking(room, LocalDate.now().plusDays(3))));

        List<RoomResponseDTO> result = roomService.findAllRooms();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bookedUntil()).isEqualTo(LocalDate.now().plusDays(3));
    }

    @Test
    void findRoomById_survivesTwoActiveBookingsForTheSameRoom() {
        UUID roomId = UUID.randomUUID();
        Room room = room(roomId, true);
        lenient().when(authorizationService.isAdmin()).thenReturn(false);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(bookingRepository.findByRoom_IdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(any(), any(), any()))
                .thenReturn(List.of(
                        booking(room, LocalDate.now().plusDays(2)),
                        booking(room, LocalDate.now().plusDays(5))));

        RoomResponseDTO result = roomService.findRoomById(roomId);

        assertThat(result.bookedUntil()).isEqualTo(LocalDate.now().plusDays(5));
    }

    @Test
    void findRoomById_hidesInactiveRoomFromNonAdmin() {
        UUID roomId = UUID.randomUUID();
        Room room = room(roomId, false);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(authorizationService.isAdmin()).thenReturn(false);

        assertThatThrownBy(() -> roomService.findRoomById(roomId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRoom_omittingRoomStatusKeepsTheExistingStatus() {
        UUID roomId = UUID.randomUUID();
        Room existing = room(roomId, true);
        existing.setRoomStatus(Status.GEBUCHT);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(existing));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room details = room(roomId, true);
        details.setRoomStatus(null);

        Room result = roomService.updateRoom(roomId, details);

        assertThat(result.getRoomStatus()).isEqualTo(Status.GEBUCHT);
    }

    @Test
    void updateRoom_providingRoomStatusUpdatesIt() {
        UUID roomId = UUID.randomUUID();
        Room existing = room(roomId, true);
        existing.setRoomStatus(Status.VERFUGBAR);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(existing));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> inv.getArgument(0));

        Room details = room(roomId, true);
        details.setRoomStatus(Status.GEBUCHT);

        Room result = roomService.updateRoom(roomId, details);

        assertThat(result.getRoomStatus()).isEqualTo(Status.GEBUCHT);
    }

    private Booking period(Room room, LocalDate start, LocalDate end) {
        Booking booking = new Booking();
        booking.setRoom(room);
        booking.setStartTime(start);
        booking.setEndTime(end);
        return booking;
    }

    @Test
    void getBookedPeriods_passesWindowToOverlapQuery_sortsAndMapsOnlyDates() {
        UUID roomId = UUID.randomUUID();
        Room room = room(roomId, true);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        when(bookingRepository.findOverlapping(roomId, from, to, null)).thenReturn(List.of(
                period(room, LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 22)),
                period(room, LocalDate.of(2026, 2, 27), LocalDate.of(2026, 3, 1)),
                period(room, LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 6))));

        List<BookedPeriodDTO> result = roomService.getBookedPeriods(roomId, from, to);

        assertThat(result).containsExactly(
                new BookedPeriodDTO(LocalDate.of(2026, 2, 27), LocalDate.of(2026, 3, 1)),
                new BookedPeriodDTO(LocalDate.of(2026, 3, 5), LocalDate.of(2026, 3, 6)),
                new BookedPeriodDTO(LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 22)));
    }

    @Test
    void getBookedPeriods_defaultsToTodayAndTodayPlus90() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room(roomId, true)));
        LocalDate today = LocalDate.now();
        when(bookingRepository.findOverlapping(roomId, today, today.plusDays(90), null)).thenReturn(List.of());

        assertThat(roomService.getBookedPeriods(roomId, null, null)).isEmpty();

        verify(bookingRepository).findOverlapping(roomId, today, today.plusDays(90), null);
    }

    @Test
    void getBookedPeriods_defaultsAreAppliedIndependently() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room(roomId, true)));
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(10);
        LocalDate to = today.plusDays(5);
        when(bookingRepository.findOverlapping(roomId, from, today.plusDays(90), null)).thenReturn(List.of());
        when(bookingRepository.findOverlapping(roomId, today, to, null)).thenReturn(List.of());

        roomService.getBookedPeriods(roomId, from, null);
        roomService.getBookedPeriods(roomId, null, to);

        verify(bookingRepository).findOverlapping(roomId, from, today.plusDays(90), null);
        verify(bookingRepository).findOverlapping(roomId, today, to, null);
    }

    @Test
    void getBookedPeriods_rejectsToBeforeFrom() {
        UUID roomId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 3, 10);

        assertThatThrownBy(() -> roomService.getBookedPeriods(roomId, from, from.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(bookingRepository, never()).findOverlapping(any(), any(), any(), any());
    }

    @Test
    void getBookedPeriods_rejectsSpanOver366Days_butAllows366() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room(roomId, true)));
        LocalDate from = LocalDate.of(2026, 1, 1);
        when(bookingRepository.findOverlapping(roomId, from, from.plusDays(366), null)).thenReturn(List.of());

        assertThatThrownBy(() -> roomService.getBookedPeriods(roomId, from, from.plusDays(367)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(roomService.getBookedPeriods(roomId, from, from.plusDays(366))).isEmpty();
    }

    @Test
    void getBookedPeriods_unknownRoomIsNotFound() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getBookedPeriods(roomId, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(bookingRepository, never()).findOverlapping(any(), any(), any(), any());
    }

    @Test
    void getBookedPeriods_inactiveRoomIsNotFoundForNonAdmin() {
        UUID roomId = UUID.randomUUID();
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room(roomId, false)));
        when(authorizationService.isAdmin()).thenReturn(false);

        assertThatThrownBy(() -> roomService.getBookedPeriods(roomId, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
