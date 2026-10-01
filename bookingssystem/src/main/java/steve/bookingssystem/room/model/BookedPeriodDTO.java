package steve.bookingssystem.room.model;

import steve.bookingssystem.booking.model.Booking;

import java.time.LocalDate;

// Deliberately only the dates: no user, bookingId or payment data (privacy).
public record BookedPeriodDTO(LocalDate startTime, LocalDate endTime) {
    public static BookedPeriodDTO from(Booking booking) {
        return new BookedPeriodDTO(booking.getStartTime(), booking.getEndTime());
    }
}
