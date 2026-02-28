package com.ticket.payment.service;

import com.ticket.common.event.BookingRequestedEvent;
import com.ticket.common.event.PaymentCompletedEvent;
import com.ticket.common.event.PaymentFailedEvent;
import com.ticket.common.exception.BusinessException;
import com.ticket.common.exception.ResourceNotFoundException;
import com.ticket.common.kafka.EventPublisher;
import com.ticket.common.kafka.KafkaTopics;
import com.ticket.common.tenant.TenantContext;
import com.ticket.payment.dto.*;
import com.ticket.payment.entity.*;
import com.ticket.payment.repository.PaymentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final BigDecimal FEE_PERCENTAGE = new BigDecimal("2.5");

    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;
    private final Random random = new Random();

    @Value("${payment.simulation.enabled:true}")
    private boolean simulationEnabled;

    @Value("${payment.simulation.success-rate:95}")
    private int successRate;

    @Value("${payment.simulation.delay-ms:1000}")
    private int simulationDelay;

    public PaymentService(PaymentRepository paymentRepository, EventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Payment createPaymentFromBooking(BookingRequestedEvent event) {
        Long bookingId = Long.valueOf(event.getBookingId());
        logger.info("Creating payment for booking {}", bookingId);

        // Check if payment already exists
        if (paymentRepository.existsByBookingIdAndStatusIn(bookingId, 
                List.of(PaymentStatus.PENDING, PaymentStatus.PROCESSING, PaymentStatus.COMPLETED))) {
            logger.warn("Payment already exists for booking {}", bookingId);
            return paymentRepository.findByBookingId(bookingId).orElse(null);
        }

        // Calculate fee
        BigDecimal fee = event.getAmount()
                .multiply(FEE_PERCENTAGE)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = event.getAmount().add(fee);

        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setConfirmationNumber(event.getIdempotencyKey());
        payment.setUserId(Long.valueOf(event.getUserId()));
        payment.setUserEmail("user@example.com"); // Event doesn't have userEmail
        payment.setEventId(Long.valueOf(event.getEventId()));
        payment.setAmount(event.getAmount());
        payment.setFee(fee);
        payment.setTotalAmount(totalAmount);
        payment.setStatus(PaymentStatus.PENDING);

        return paymentRepository.save(payment);
    }

    @Transactional
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "processPaymentFallback")
    public PaymentResponse processPayment(ProcessPaymentRequest request, Long userId) {
        logger.info("Processing payment for booking {} by user {}", request.bookingId(), userId);

        Payment payment = paymentRepository.findByBookingIdWithLock(request.bookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "bookingId", request.bookingId()));

        // Validate user
        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException("You don't have permission to process this payment");
        }

        // Check status
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Payment is not in pending status");
        }

        payment.setPaymentMethod(request.paymentMethod());
        payment.setStatus(PaymentStatus.PROCESSING);
        paymentRepository.save(payment);

        // Process with payment gateway (simulated)
        PaymentGatewayResult result = processWithGateway(payment, request);

        if (result.success()) {
            payment.markCompleted(result.transactionReference(), result.gatewayResponse());
            paymentRepository.save(payment);
            publishPaymentCompleted(payment);
            logger.info("Payment completed for booking {} with reference {}", 
                       payment.getBookingId(), payment.getTransactionReference());
        } else {
            payment.markFailed(result.failureReason());
            paymentRepository.save(payment);
            publishPaymentFailed(payment);
            logger.warn("Payment failed for booking {}: {}", 
                       payment.getBookingId(), payment.getFailureReason());
        }

        return PaymentMapper.toResponse(payment);
    }

    public PaymentResponse processPaymentFallback(ProcessPaymentRequest request, Long userId, Throwable t) {
        logger.error("Payment gateway circuit breaker triggered: {}", t.getMessage());
        throw new BusinessException("Payment service is temporarily unavailable. Please try again later.");
    }

    private PaymentGatewayResult processWithGateway(Payment payment, ProcessPaymentRequest request) {
        if (simulationEnabled) {
            return simulatePaymentGateway(payment);
        }
        // In production, integrate with actual payment gateway here
        return new PaymentGatewayResult(false, null, null, "Payment gateway not configured");
    }

    private PaymentGatewayResult simulatePaymentGateway(Payment payment) {
        try {
            // Simulate processing time
            Thread.sleep(simulationDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simulate success/failure based on configured rate
        boolean success = random.nextInt(100) < successRate;

        if (success) {
            String transactionRef = "TXN" + System.currentTimeMillis() + 
                                   String.format("%06d", random.nextInt(1000000));
            String response = "{\"status\":\"approved\",\"authCode\":\"" + 
                             UUID.randomUUID().toString().substring(0, 8).toUpperCase() + "\"}";
            return new PaymentGatewayResult(true, transactionRef, response, null);
        } else {
            String[] failureReasons = {
                "Insufficient funds",
                "Card declined",
                "Transaction timeout",
                "Invalid card details",
                "Suspected fraud"
            };
            String reason = failureReasons[random.nextInt(failureReasons.length)];
            return new PaymentGatewayResult(false, null, null, reason);
        }
    }

    private void publishPaymentCompleted(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            TenantContext.getTenantId(),
            UUID.randomUUID().toString(),
            payment.getId().toString(),
            payment.getBookingId().toString(),
            payment.getAmount(),
            payment.getTransactionReference(),
            payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null
        );
        eventPublisher.publish(KafkaTopics.PAYMENT_COMPLETED, 
                              payment.getBookingId().toString(), event);
    }

    private void publishPaymentFailed(Payment payment) {
        PaymentFailedEvent event = new PaymentFailedEvent(
            TenantContext.getTenantId(),
            UUID.randomUUID().toString(),
            payment.getId().toString(),
            payment.getBookingId().toString(),
            payment.getAmount(),
            payment.getFailureReason(),
            "PAYMENT_FAILED"
        );
        eventPublisher.publish(KafkaTopics.PAYMENT_FAILED, 
                              payment.getBookingId().toString(), event);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long paymentId, Long userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException("You don't have permission to view this payment");
        }

        return PaymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBooking(Long bookingId, Long userId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "bookingId", bookingId));

        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException("You don't have permission to view this payment");
        }

        return PaymentMapper.toResponse(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> getUserPayments(Long userId, Pageable pageable) {
        return paymentRepository.findByUserId(userId, pageable)
                .map(PaymentMapper::toResponse);
    }

    @Transactional
    public PaymentResponse initiateRefund(Long paymentId, Long userId, BigDecimal amount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (!payment.getUserId().equals(userId)) {
            throw new BusinessException("You don't have permission to refund this payment");
        }

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BusinessException("Only completed payments can be refunded");
        }

        if (amount == null || amount.compareTo(payment.getAmount()) > 0) {
            amount = payment.getAmount();
        }

        payment.initiateRefund(amount);
        
        // Simulate refund processing
        if (simulationEnabled) {
            simulateRefund(payment);
        }

        paymentRepository.save(payment);
        logger.info("Refund initiated for payment {} with amount {}", paymentId, amount);
        
        return PaymentMapper.toResponse(payment);
    }

    private void simulateRefund(Payment payment) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean success = random.nextInt(100) < 98; // 98% success rate for refunds
        if (success) {
            String refundRef = "REF" + System.currentTimeMillis();
            payment.completeRefund(refundRef);
            payment.setStatus(PaymentStatus.REFUNDED);
        } else {
            payment.setRefundStatus(RefundStatus.FAILED);
        }
    }

    // Helper methods for internal use and testing
    public Payment createPayment(Long bookingId, Long userId, BigDecimal amount, PaymentMethod method) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepository.save(payment);
    }

    public Optional<Payment> findById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }

    public Optional<Payment> findByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }

    @Transactional
    public Payment markCompleted(Long paymentId, String transactionReference) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionReference(transactionReference);
        payment.setCompletedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment markFailed(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        return paymentRepository.save(payment);
    }

    public String generateTransactionReference() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    // Record for gateway result
    private record PaymentGatewayResult(
        boolean success,
        String transactionReference,
        String gatewayResponse,
        String failureReason
    ) {}
}
