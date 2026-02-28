package com.ticket.booking.repository;

import com.ticket.booking.entity.BookingSaga;
import com.ticket.booking.entity.SagaStatus;
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
public interface BookingSagaRepository extends JpaRepository<BookingSaga, Long> {

    Optional<BookingSaga> findByBookingId(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM BookingSaga s WHERE s.bookingId = :bookingId")
    Optional<BookingSaga> findByBookingIdWithLock(@Param("bookingId") Long bookingId);

    List<BookingSaga> findByStatus(SagaStatus status);

    @Query("SELECT s FROM BookingSaga s WHERE s.status = 'STARTED' AND s.expiresAt < :now")
    List<BookingSaga> findExpiredSagas(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM BookingSaga s WHERE s.status IN ('STARTED', 'IN_PROGRESS') AND s.retryCount < :maxRetries")
    List<BookingSaga> findSagasForRetry(@Param("maxRetries") int maxRetries);

    @Query("SELECT s FROM BookingSaga s WHERE s.status = 'COMPENSATING'")
    List<BookingSaga> findCompensatingSagas();
}
