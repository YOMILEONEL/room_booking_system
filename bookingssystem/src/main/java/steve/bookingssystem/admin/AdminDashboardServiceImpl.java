package steve.bookingssystem.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import steve.bookingssystem.booking.model.Booking;
import steve.bookingssystem.booking.model.BookingResponseDTO;
import steve.bookingssystem.booking.repository.BookingRepository;
import steve.bookingssystem.payment.model.Payment;
import steve.bookingssystem.payment.model.PaymentStatus;
import steve.bookingssystem.payment.repository.PaymentRepository;
import steve.bookingssystem.room.model.Status;
import steve.bookingssystem.room.repository.RoomRepository;
import steve.bookingssystem.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminDashboardServiceImpl implements AdminDashboardService {

    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookingRepository bookingRepository;
    @Autowired
    private PaymentRepository paymentRepository;

    @Override
    public AdminDashboardDto getDashboard() {
        long availableRooms = roomRepository.countByRoomStatus(Status.VERFUGBAR);
        long bookedRooms = roomRepository.countByRoomStatus(Status.GEBUCHT);
        long totalUsers = userRepository.count();
        long upcomingBookings = bookingRepository.countByStartTimeGreaterThanEqual(LocalDate.now());

        LocalDate today = LocalDate.now();
        ZoneId zone = ZoneId.systemDefault();
        var monthStart = today.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay(zone).toInstant();
        var monthEnd = today.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1).atStartOfDay(zone).toInstant();
        BigDecimal revenueThisMonth = paymentRepository.sumRevenueBetween(monthStart, monthEnd);

        List<BookingResponseDTO> recentBookings = bookingRepository.findTop5ByOrderByCreatedAtDesc()
                .stream().map(BookingResponseDTO::from).toList();

        List<Booking> allBookings = bookingRepository.findAll();

        List<RoomBookingCountDto> topRooms = allBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getRoom().getName(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new RoomBookingCountDto(e.getKey(), e.getValue()))
                .toList();

        List<CustomerBookingCountDto> topCustomers = allBookings.stream()
                .collect(Collectors.groupingBy(b -> b.getUser().getUsername(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> new CustomerBookingCountDto(e.getKey(), e.getValue()))
                .toList();

        List<BookingResponseDTO> pendingPayments = paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .map(p -> BookingResponseDTO.from(p.getBooking()))
                .toList();

        return new AdminDashboardDto(availableRooms, bookedRooms, totalUsers, upcomingBookings,
                revenueThisMonth, recentBookings, pendingPayments, topRooms, topCustomers);
    }
}
