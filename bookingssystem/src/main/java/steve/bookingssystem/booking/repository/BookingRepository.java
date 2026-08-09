package steve.bookingssystem.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import steve.bookingssystem.booking.model.Booking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findByUser_Id(UUID userId);

    long countByStartTimeGreaterThanEqual(LocalDate date);

    List<Booking> findTop5ByOrderByCreatedAtDesc();

    List<Booking> findByStartTimeLessThanEqualAndEndTimeGreaterThanEqual(LocalDate start, LocalDate end);

    // List, not Optional<Booking>: a query matching more than one active booking for the same
    // room (e.g. via the still-open 1.5 overlap race) would make Spring Data throw
    // IncorrectResultSizeDataAccessException for a singular return type - the caller decides
    // how to pick one instead of the query crashing outright.
    List<Booking> findByRoom_IdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            UUID roomId, LocalDate start, LocalDate end);

    @Query("select b from Booking b where b.room.id = :roomId "
            + "and b.startTime <= :endTime and b.endTime >= :startTime "
            + "and (:excludeBookingId is null or b.bookingId <> :excludeBookingId)")
    List<Booking> findOverlapping(
            @Param("roomId") UUID roomId,
            @Param("startTime") LocalDate startTime,
            @Param("endTime") LocalDate endTime,
            @Param("excludeBookingId") UUID excludeBookingId
    );
}
