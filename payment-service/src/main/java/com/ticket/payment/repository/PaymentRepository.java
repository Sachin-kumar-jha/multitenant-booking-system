package com.ticket.payment.repository;

import com.ticket.payment.entity.Payment;
import com.ticket.payment.entity.PaymentStatus;
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
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByBookingId(Long bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.bookingId = :bookingId")
    Optional<Payment> findByBookingIdWithLock(@Param("bookingId") Long bookingId);

    Optional<Payment> findByTransactionReference(String transactionReference);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    Page<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status, Pageable pageable);

    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :before")
    List<Payment> findStalePendingPayments(@Param("before") LocalDateTime before);

    @Query("SELECT p FROM Payment p WHERE p.status = 'PROCESSING' AND p.createdAt < :before")
    List<Payment> findStaleProcessingPayments(@Param("before") LocalDateTime before);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.eventId = :eventId AND p.status = 'COMPLETED'")
    java.math.BigDecimal sumCompletedPaymentsForEvent(@Param("eventId") Long eventId);

    boolean existsByBookingIdAndStatusIn(Long bookingId, List<PaymentStatus> statuses);
}
