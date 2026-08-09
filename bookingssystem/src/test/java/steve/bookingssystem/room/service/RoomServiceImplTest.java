package steve.bookingssystem.room.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.repository.BookingRepository;
import steve.bookingssystem.exception.ResourceNotFoundException;
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
}
