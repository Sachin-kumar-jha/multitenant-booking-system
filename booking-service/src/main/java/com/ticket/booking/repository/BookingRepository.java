package com.ticket.booking.repository;

import com.ticket.booking.entity.Booking;
import com.ticket.booking.entity.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByConfirmationNumber(String confirmationNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdWithLock(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.confirmationNumber = :confirmationNumber")
    Optional<Booking> findByConfirmationNumberWithLock(@Param("confirmationNumber") String confirmationNumber);

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByUserIdAndStatus(Long userId, BookingStatus status, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.userId = :userId ORDER BY b.createdAt DESC")
    List<Booking> findRecentBookingsByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.eventId = :eventId")
    Page<Booking> findByEventId(@Param("eventId") Long eventId, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.eventId = :eventId AND b.status = :status")
    List<Booking> findByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.expiresAt < :now")
    List<Booking> findExpiredBookings(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.userId = :userId AND b.eventId = :eventId AND b.status IN ('PENDING', 'CONFIRMED')")
    long countActiveBookingsByUserAndEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);

    @Query("SELECT SUM(SIZE(b.items)) FROM Booking b WHERE b.userId = :userId AND b.eventId = :eventId AND b.status IN ('PENDING', 'CONFIRMED')")
    Long countTicketsByUserAndEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);

    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.createdAt < :before")
    List<Booking> findByStatusAndCreatedBefore(@Param("status") BookingStatus status, @Param("before") LocalDateTime before);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.eventId = :eventId AND b.status = 'CONFIRMED'")
    long countConfirmedBookingsForEvent(@Param("eventId") Long eventId);

    boolean existsByLockId(String lockId);
    
    @Query("SELECT b FROM Booking b WHERE b.userId = :userId ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
}
