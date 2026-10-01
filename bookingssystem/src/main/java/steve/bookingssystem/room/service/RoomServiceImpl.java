package steve.bookingssystem.room.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.repository.BookingRepository;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.room.model.BookedPeriodDTO;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.model.RoomResponseDTO;
import steve.bookingssystem.room.repository.RoomRepository;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.user.model.CustomerType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    public RoomRepository roomRepository;
    @Autowired
    public AuthorizationService authorizationService;
    @Autowired
    public BookingRepository bookingRepository;

    @Override
    public Room saveRoom(Room room) {
        authorizationService.requireAdmin();
        return roomRepository.save(room);
    }

    @Override
    public RoomResponseDTO findRoomById(UUID id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));

        if (!room.isActive() && !authorizationService.isAdmin()) {
            throw new ResourceNotFoundException("Room not found: " + id);
        }

        LocalDate today = LocalDate.now();
        LocalDate bookedUntil = bookingRepository
                .findByRoom_IdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(id, today, today)
                .stream()
                .map(Booking::getEndTime)
                .max(LocalDate::compareTo)
                .orElse(null);

        BigDecimal effectivePrice = effectivePrice(room.getPricePerDay());

        return RoomResponseDTO.from(room, bookedUntil, effectivePrice);
    }

    @Override
    public List<RoomResponseDTO> findAllRooms() {
        List<Room> rooms = authorizationService.isAdmin()
                ? roomRepository.findAll()
                : roomRepository.findByActiveTrue();

        LocalDate today = LocalDate.now();
        // Merge function instead of the default Collectors.toMap(keyMapper, valueMapper), which
        // throws IllegalStateException("Duplicate key ...") the moment two active bookings exist
        // for the same room today (e.g. via the 1.5 overlap race) - that exception used to take
        // down the entire room list (GET /room/Get -> 409) for every user. Keep the later end
        // date, since that's the more useful "booked until" value to show.
        Map<UUID, LocalDate> bookedUntilByRoom = bookingRepository
                .findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(today, today)
                .stream()
                .collect(Collectors.toMap(
                        b -> b.getRoom().getId(),
                        Booking::getEndTime,
                        (existing, candidate) -> existing.isAfter(candidate) ? existing : candidate));

        CustomerType customerType = authorizationService.currentCustomerType();

        return rooms.stream()
                .map(room -> RoomResponseDTO.from(
                        room,
                        bookedUntilByRoom.get(room.getId()),
                        customerType != null ? customerType.applyPricing(room.getPricePerDay()) : room.getPricePerDay()))
                .toList();
    }

    private BigDecimal effectivePrice(BigDecimal pricePerDay) {
        CustomerType customerType = authorizationService.currentCustomerType();
        return customerType != null ? customerType.applyPricing(pricePerDay) : pricePerDay;
    }

    @Override
    public Room updateRoom(UUID id, Room roomDetails) {
        authorizationService.requireAdmin();
        Room roomExist = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));

        roomExist.setName(roomDetails.getName());
        roomExist.setCapacity(roomDetails.getCapacity());
        roomExist.setSizeSquareMeters(roomDetails.getSizeSquareMeters());
        roomExist.setLocation(roomDetails.getLocation());
        roomExist.setCity(roomDetails.getCity());
        roomExist.setDescription(roomDetails.getDescription());
        roomExist.setPricePerDay(roomDetails.getPricePerDay());
        // Guarded like the other optional fields below it conceptually should be: a request body
        // that omits roomStatus deserializes it as null, and an unconditional set would silently
        // wipe the room's current status instead of leaving it untouched.
        if (roomDetails.getRoomStatus() != null) {
            roomExist.setRoomStatus(roomDetails.getRoomStatus());
        }
        return roomRepository.save(roomExist);
    }

    @Override
    public Room activate(UUID id) {
        authorizationService.requireAdmin();
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        room.setActive(true);
        return roomRepository.save(room);
    }

    @Override
    public Room deactivate(UUID id) {
        authorizationService.requireAdmin();
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));
        room.setActive(false);
        return roomRepository.save(room);
    }

    private static final int DEFAULT_WINDOW_DAYS = 90;
    private static final long MAX_WINDOW_DAYS = 366;

    @Override
    public List<BookedPeriodDTO> getBookedPeriods(UUID roomId, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        LocalDate windowFrom = from != null ? from : today;
        LocalDate windowTo = to != null ? to : today.plusDays(DEFAULT_WINDOW_DAYS);

        if (windowTo.isBefore(windowFrom)) {
            throw new IllegalArgumentException("'to' darf nicht vor 'from' liegen.");
        }
        if (ChronoUnit.DAYS.between(windowFrom, windowTo) > MAX_WINDOW_DAYS) {
            throw new IllegalArgumentException("Der Zeitraum darf höchstens " + MAX_WINDOW_DAYS + " Tage umfassen.");
        }

        // Same visibility rule as findRoomById: unknown, or inactive and not admin -> 404.
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomId));
        if (!room.isActive() && !authorizationService.isAdmin()) {
            throw new ResourceNotFoundException("Room not found: " + roomId);
        }

        return bookingRepository.findOverlapping(roomId, windowFrom, windowTo, null)
                .stream()
                .map(BookedPeriodDTO::from)
                .sorted(Comparator.comparing(BookedPeriodDTO::startTime)
                        .thenComparing(BookedPeriodDTO::endTime))
                .toList();
    }
}
