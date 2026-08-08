package steve.bookingssystem.room.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.repository.BookingRepository;
import steve.bookingssystem.exception.ResourceNotFoundException;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.room.model.RoomResponseDTO;
import steve.bookingssystem.room.repository.RoomRepository;
import steve.bookingssystem.security.AuthorizationService;
import steve.bookingssystem.user.model.CustomerType;

import java.math.BigDecimal;
import java.time.LocalDate;
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
                .map(Booking::getEndTime)
                .orElse(null);

        BigDecimal effectivePrice = effectivePrice(room.getPricePerNight());

        return RoomResponseDTO.from(room, bookedUntil, effectivePrice);
    }

    @Override
    public List<RoomResponseDTO> findAllRooms() {
        List<Room> rooms = authorizationService.isAdmin()
                ? roomRepository.findAll()
                : roomRepository.findByActiveTrue();

        LocalDate today = LocalDate.now();
        Map<UUID, LocalDate> bookedUntilByRoom = bookingRepository
                .findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(today, today)
                .stream()
                .collect(Collectors.toMap(b -> b.getRoom().getId(), Booking::getEndTime));

        CustomerType customerType = authorizationService.currentCustomerType();

        return rooms.stream()
                .map(room -> RoomResponseDTO.from(
                        room,
                        bookedUntilByRoom.get(room.getId()),
                        customerType != null ? customerType.applyPricing(room.getPricePerNight()) : room.getPricePerNight()))
                .toList();
    }

    private BigDecimal effectivePrice(BigDecimal pricePerNight) {
        CustomerType customerType = authorizationService.currentCustomerType();
        return customerType != null ? customerType.applyPricing(pricePerNight) : pricePerNight;
    }

    @Override
    public Room updateRoom(UUID id, Room roomDetails) {
        authorizationService.requireAdmin();
        Room roomExist = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found: " + id));

        roomExist.setName(roomDetails.getName());
        roomExist.setCapacity(roomDetails.getCapacity());
        roomExist.setLocation(roomDetails.getLocation());
        roomExist.setPricePerNight(roomDetails.getPricePerNight());
        roomExist.setRoomStatus(roomDetails.getRoomStatus());
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
}
