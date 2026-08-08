package steve.bookingssystem.admin;

import steve.bookingssystem.booking.model.BookingResponseDTO;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardDto(
        long availableRooms,
        long bookedRooms,
        long totalUsers,
        long upcomingBookings,
        BigDecimal revenueThisMonth,
        List<BookingResponseDTO> recentBookings,
        List<BookingResponseDTO> pendingPayments,
        List<RoomBookingCountDto> topRooms,
        List<CustomerBookingCountDto> topCustomers
) {
}
