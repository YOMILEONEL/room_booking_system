package steve.bookingssystem.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import steve.bookingssystem.booking.model.Booking;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser_Id(Long userId);

    @Query("select b from Booking b where b.room.id = :roomId "
            + "and b.startTime <= :endTime and b.endTime >= :startTime "
            + "and (:excludeBookingId is null or b.bookingId <> :excludeBookingId)")
    List<Booking> findOverlapping(
            @Param("roomId") Long roomId,
            @Param("startTime") LocalDate startTime,
            @Param("endTime") LocalDate endTime,
            @Param("excludeBookingId") Long excludeBookingId
    );
}
