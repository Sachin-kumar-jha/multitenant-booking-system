package com.ticket.event.repository;

import com.ticket.event.entity.Seat;
import com.ticket.event.entity.SeatStatus;
import com.ticket.event.entity.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :id")
    Optional<Seat> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :ids")
    List<Seat> findAllByIdWithLock(@Param("ids") List<Long> ids);

    List<Seat> findByEventId(Long eventId);

    @Query("SELECT s FROM Seat s WHERE s.event.id = :eventId AND s.status = :status")
    List<Seat> findByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") SeatStatus status);

    @Query("SELECT s FROM Seat s WHERE s.event.id = :eventId AND s.section = :section ORDER BY s.rowNumber, s.seatNumber")
    List<Seat> findByEventIdAndSection(@Param("eventId") Long eventId, @Param("section") String section);

    @Query("SELECT s FROM Seat s WHERE s.event.id = :eventId AND s.type = :type AND s.status = 'AVAILABLE'")
    List<Seat> findAvailableSeatsByType(@Param("eventId") Long eventId, @Param("type") SeatType type);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.event.id = :eventId AND s.status = 'AVAILABLE'")
    long countAvailableSeats(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.event.id = :eventId AND s.status = :status")
    long countByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") SeatStatus status);

    @Query("SELECT DISTINCT s.section FROM Seat s WHERE s.event.id = :eventId")
    List<String> findSectionsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT s FROM Seat s WHERE s.status = 'LOCKED' AND s.lockedUntil < :now")
    List<Seat> findExpiredLocks(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.lockedBy = null, s.lockedUntil = null WHERE s.status = 'LOCKED' AND s.lockedUntil < :now")
    int releaseExpiredLocks(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Seat s WHERE s.lockedBy = :userId AND s.event.id = :eventId AND s.status = 'LOCKED'")
    List<Seat> findLockedSeatsByUserAndEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);

    @Query("SELECT s FROM Seat s WHERE s.bookingId = :bookingId")
    List<Seat> findByBookingId(@Param("bookingId") Long bookingId);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.bookingId = null WHERE s.bookingId = :bookingId")
    int releaseBookedSeats(@Param("bookingId") Long bookingId);

    @Query("SELECT s FROM Seat s WHERE s.event.id = :eventId AND s.section = :section AND s.rowNumber = :rowNumber ORDER BY s.seatNumber")
    List<Seat> findByEventIdAndSectionAndRow(@Param("eventId") Long eventId, @Param("section") String section, @Param("rowNumber") String rowNumber);

    boolean existsByEventIdAndSectionAndRowNumberAndSeatNumber(Long eventId, String section, String rowNumber, String seatNumber);
}
