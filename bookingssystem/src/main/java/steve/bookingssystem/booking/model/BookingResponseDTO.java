package steve.bookingssystem.booking.model;

import lombok.Data;
import steve.bookingssystem.payment.model.PaymentResponseDTO;
import steve.bookingssystem.room.model.Room;
import steve.bookingssystem.user.model.UserDTO;

import java.time.LocalDate;

@Data
public class BookingResponseDTO {

    private Long bookingId;
    private Room room;
    private UserDTO user;
    private LocalDate startTime;
    private LocalDate endTime;
    private PaymentResponseDTO payment;

    public BookingResponseDTO(Long bookingId, Room room, UserDTO user, LocalDate startTime, LocalDate endTime, PaymentResponseDTO payment) {
        this.bookingId = bookingId;
        this.room = room;
        this.user = user;
        this.startTime = startTime;
        this.endTime = endTime;
        this.payment = payment;
    }

    public static BookingResponseDTO from(Booking booking) {
        return new BookingResponseDTO(
                booking.getBookingId(),
                booking.getRoom(),
                steve.bookingssystem.user.model.User.getUserDTO(booking.getUser()),
                booking.getStartTime(),
                booking.getEndTime(),
                PaymentResponseDTO.from(booking.getPayment())
        );
    }
}
