package com.ticket.booking.service;

import com.ticket.booking.client.EventServiceClient;
import com.ticket.booking.dto.*;
import com.ticket.booking.entity.*;
import com.ticket.booking.repository.BookingRepository;
import com.ticket.booking.repository.BookingSagaRepository;
import com.ticket.common.event.BookingRequestedEvent;
import com.ticket.common.event.BookingConfirmedEvent;
import com.ticket.common.event.BookingCancelledEvent;
import com.ticket.common.exception.BusinessException;
import com.ticket.common.exception.ResourceNotFoundException;
import com.ticket.common.kafka.EventPublisher;
import com.ticket.common.kafka.KafkaTopics;
import com.ticket.common.tenant.TenantContext;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookingSagaRepository sagaRepository;
    private final EventServiceClient eventServiceClient;
    private final EventPublisher eventPublisher;

    @Value("${booking.payment-timeout-seconds:300}")
    private int paymentTimeoutSeconds;

    @Value("${booking.max-retries:3}")
    private int maxRetries;

    public BookingService(BookingRepository bookingRepository,
                         BookingSagaRepository sagaRepository,
                         EventServiceClient eventServiceClient,
                         EventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.sagaRepository = sagaRepository;
        this.eventServiceClient = eventServiceClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @CircuitBreaker(name = "eventService", fallbackMethod = "createBookingFallback")
    @Retry(name = "eventService")
    public BookingResponse createBooking(CreateBookingRequest request, Long userId, 
                                        String userEmail, String userName) {
        logger.info("Creating booking for user {} with lock {}", userId, request.lockId());

        // Check if booking already exists for this lock
        if (bookingRepository.existsByLockId(request.lockId())) {
            throw new BusinessException("A booking already exists for this lock");
        }

        // Validate lock with event service
        Map<String, Object> lockValidation = new HashMap<>();
        lockValidation.put("lockId", request.lockId());
        lockValidation.put("userId", userId);
        lockValidation.put("seatIds", request.seatIds());
        
        Map<String, Boolean> validationResult = eventServiceClient.validateLock(lockValidation);
        if (!validationResult.getOrDefault("valid", false)) {
            throw new BusinessException("Seat lock is invalid or expired. Please select seats again.");
        }

        // Get event data
        EventData event = eventServiceClient.getEvent(request.eventId());
        if (!"PUBLISHED".equals(event.status())) {
            throw new BusinessException("Event is not available for booking");
        }

        // Check max tickets per user
        Long existingTickets = bookingRepository.countTicketsByUserAndEvent(userId, request.eventId());
        if (existingTickets == null) existingTickets = 0L;
        if (existingTickets + request.seatIds().size() > event.maxTicketsPerUser()) {
            throw new BusinessException(String.format(
                "You can only book up to %d tickets for this event. You already have %d.",
                event.maxTicketsPerUser(), existingTickets
            ));
        }

        // Get seat details
        List<SeatData> allSeats = eventServiceClient.getSeats(request.eventId());
        Map<Long, SeatData> seatMap = new HashMap<>();
        for (SeatData seat : allSeats) {
            seatMap.put(seat.id(), seat);
        }

        // Calculate totals and create booking items
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<BookingItem> items = new ArrayList<>();

        for (Long seatId : request.seatIds()) {
            SeatData seat = seatMap.get(seatId);
            if (seat == null) {
                throw new BusinessException("Seat " + seatId + " not found");
            }

            BookingItem item = new BookingItem(
                seatId,
                seat.section(),
                seat.rowNumber(),
                seat.seatNumber(),
                seat.type(),
                seat.finalPrice()
            );
            item.setTicketCode(generateTicketCode());
            items.add(item);
            totalAmount = totalAmount.add(seat.finalPrice());
        }

        // Apply promo code discount (placeholder)
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.promoCode() != null && !request.promoCode().isEmpty()) {
            // TODO: Implement promo code validation
            discountAmount = BigDecimal.ZERO;
        }
        BigDecimal finalAmount = totalAmount.subtract(discountAmount);

        // Create booking
        Booking booking = new Booking();
        booking.setConfirmationNumber(generateConfirmationNumber());
        booking.setUserId(userId);
        booking.setUserEmail(userEmail);
        booking.setUserName(userName);
        booking.setEventId(request.eventId());
        booking.setEventName(event.name());
        booking.setEventDate(event.eventDate());
        booking.setVenue(event.venue());
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalAmount(totalAmount);
        booking.setDiscountAmount(discountAmount);
        booking.setFinalAmount(finalAmount);
        booking.setLockId(request.lockId());
        booking.setExpiresAt(LocalDateTime.now().plusSeconds(paymentTimeoutSeconds));

        for (BookingItem item : items) {
            booking.addItem(item);
        }

        booking = bookingRepository.save(booking);

        // Create saga
        BookingSaga saga = new BookingSaga(booking.getId(), booking.getExpiresAt());
        saga.setStatus(SagaStatus.IN_PROGRESS);
        sagaRepository.save(saga);

        // Publish BookingRequested event for payment service
        publishBookingRequested(booking);

        logger.info("Booking created with confirmation number: {}", booking.getConfirmationNumber());
        return BookingMapper.toResponse(booking);
    }

    public BookingResponse createBookingFallback(CreateBookingRequest request, Long userId, 
                                                  String userEmail, String userName, Throwable t) {
        logger.error("Circuit breaker fallback for createBooking: {}", t.getMessage());
        throw new BusinessException("Booking service is temporarily unavailable. Please try again later.");
    }

    private void publishBookingRequested(Booking booking) {
        List<Long> seatIds = booking.getItems().stream()
                .map(BookingItem::getSeatId)
                .toList();

        BookingRequestedEvent event = new BookingRequestedEvent(
            TenantContext.getTenantId(),
            UUID.randomUUID().toString(),
            booking.getId().toString(),
            booking.getUserId().toString(),
            booking.getEventId().toString(),
            seatIds.isEmpty() ? "" : seatIds.get(0).toString(),
            booking.getFinalAmount(),
            booking.getConfirmationNumber(),
            java.time.Instant.now().plusSeconds(900)  // 15 min expiry
        );
        eventPublisher.publish(KafkaTopics.BOOKING_REQUESTED, 
                              booking.getId().toString(), event);
        
        // Update saga
        sagaRepository.findByBookingId(booking.getId())
                .ifPresent(saga -> {
                    saga.markPaymentRequestSent();
                    sagaRepository.save(saga);
                });
    }

    @Transactional
    public void handlePaymentCompleted(Long bookingId, Long paymentId, String paymentReference) {
        logger.info("Handling payment completed for booking {}", bookingId);

        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            logger.warn("Booking {} is not in PENDING status, current: {}", bookingId, booking.getStatus());
            return;
        }

        // Book seats in event service
        Map<String, Object> bookRequest = new HashMap<>();
        bookRequest.put("seatIds", booking.getItems().stream()
                .map(BookingItem::getSeatId).toList());
        bookRequest.put("bookingId", bookingId);
        
        try {
            eventServiceClient.bookSeats(bookRequest);
        } catch (Exception e) {
            logger.error("Failed to book seats for booking {}: {}", bookingId, e.getMessage());
            // Start compensation
            compensateBooking(bookingId, "Failed to confirm seats: " + e.getMessage());
            return;
        }

        // Confirm booking
        booking.confirm(paymentId, paymentReference);
        bookingRepository.save(booking);

        // Update saga
        BookingSaga saga = sagaRepository.findByBookingIdWithLock(bookingId).orElse(null);
        if (saga != null) {
            saga.markPaymentCompleted();
            saga.markSeatsBooked();
            saga.complete();
            sagaRepository.save(saga);
        }

        // Publish confirmation event
        publishBookingConfirmed(booking);

        logger.info("Booking {} confirmed with payment {}", booking.getConfirmationNumber(), paymentReference);
    }

    @Transactional
    public void handlePaymentFailed(Long bookingId, String reason) {
        logger.info("Handling payment failed for booking {}: {}", bookingId, reason);

        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            return;
        }

        booking.setStatus(BookingStatus.FAILED);
        booking.setCancellationReason("Payment failed: " + reason);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Release seats
        releaseSeats(booking);

        // Update saga
        sagaRepository.findByBookingId(bookingId).ifPresent(saga -> {
            saga.fail(reason);
            sagaRepository.save(saga);
        });

        // Publish cancellation
        publishBookingCancelled(booking);
    }

    private void publishBookingConfirmed(Booking booking) {
        List<Long> seatIds = booking.getItems().stream()
                .map(BookingItem::getSeatId)
                .toList();

        String seatInfo = booking.getItems().stream()
                .map(item -> item.getSeatSection() + " " + item.getSeatRow() + " " + item.getSeatNumber())
                .findFirst().orElse("");

        BookingConfirmedEvent event = new BookingConfirmedEvent(
            TenantContext.getTenantId(),
            UUID.randomUUID().toString(),
            booking.getId().toString(),
            booking.getUserId().toString(),
            booking.getEventId().toString(),
            booking.getEventName(),
            seatIds.isEmpty() ? "" : seatIds.get(0).toString(),
            seatInfo,
            booking.getFinalAmount(),
            booking.getConfirmationNumber()
        );
        eventPublisher.publish(KafkaTopics.BOOKING_CONFIRMED, 
                              booking.getId().toString(), event);
    }

    private void publishBookingCancelled(Booking booking) {
        List<Long> seatIds = booking.getItems().stream()
                .map(BookingItem::getSeatId)
                .toList();

        BookingCancelledEvent event = new BookingCancelledEvent(
            TenantContext.getTenantId(),
            UUID.randomUUID().toString(),
            booking.getId().toString(),
            booking.getUserId().toString(),
            booking.getEventId().toString(),
            seatIds.isEmpty() ? "" : seatIds.get(0).toString(),
            booking.getCancellationReason(),
            booking.getStatus() == BookingStatus.CONFIRMED  // refund required if was confirmed
        );
        eventPublisher.publish(KafkaTopics.BOOKING_CANCELLED, 
                              booking.getId().toString(), event);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long userId, String reason) {
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("You don't have permission to cancel this booking");
        }

        if (!booking.canBeCancelled()) {
            throw new BusinessException("This booking cannot be cancelled");
        }

        booking.cancel(reason);
        
        // Release seats if booking was confirmed
        if (booking.getPaymentId() != null) {
            releaseSeats(booking);
            // TODO: Trigger refund process
            booking.setRefundStatus(RefundStatus.PENDING);
        }

        bookingRepository.save(booking);
        publishBookingCancelled(booking);

        logger.info("Booking {} cancelled by user {}", booking.getConfirmationNumber(), userId);
        return BookingMapper.toResponse(booking);
    }

    private void compensateBooking(Long bookingId, String reason) {
        logger.info("Compensating booking {}: {}", bookingId, reason);

        Booking booking = bookingRepository.findByIdWithLock(bookingId).orElse(null);
        if (booking == null) return;

        booking.setStatus(BookingStatus.FAILED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        bookingRepository.save(booking);

        // Release seats
        releaseSeats(booking);

        // Update saga
        sagaRepository.findByBookingIdWithLock(bookingId).ifPresent(saga -> {
            saga.compensate();
            saga.compensated();
            saga.setLastError(reason);
            sagaRepository.save(saga);
        });
    }

    private void releaseSeats(Booking booking) {
        try {
            Map<String, Object> releaseRequest = new HashMap<>();
            releaseRequest.put("bookingId", booking.getId());
            eventServiceClient.releaseSeats(releaseRequest);
        } catch (Exception e) {
            logger.error("Failed to release seats for booking {}: {}", 
                        booking.getId(), e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public BookingResponse getBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("You don't have permission to view this booking");
        }

        return BookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByConfirmation(String confirmationNumber, Long userId) {
        Booking booking = bookingRepository.findByConfirmationNumber(confirmationNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "confirmationNumber", confirmationNumber));

        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("You don't have permission to view this booking");
        }

        return BookingMapper.toResponse(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(Long userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable)
                .map(BookingMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookingsByStatus(Long userId, BookingStatus status, Pageable pageable) {
        return bookingRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(BookingMapper::toResponse);
    }

    // Scheduled task to expire pending bookings
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePendingBookings() {
        List<Booking> expiredBookings = bookingRepository.findExpiredBookings(LocalDateTime.now());
        
        for (Booking booking : expiredBookings) {
            logger.info("Expiring booking {}", booking.getConfirmationNumber());
            booking.expire();
            bookingRepository.save(booking);
            releaseSeats(booking);
            publishBookingCancelled(booking);
        }

        if (!expiredBookings.isEmpty()) {
            logger.info("Expired {} pending bookings", expiredBookings.size());
        }
    }

    private String generateConfirmationNumber() {
        return "BK-" + System.currentTimeMillis() + 
               String.format("%04d", new Random().nextInt(10000));
    }

    private String generateTicketCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    // Helper methods for testing
    public Optional<Booking> findById(Long id) {
        return bookingRepository.findById(id);
    }

    public Optional<Booking> findByConfirmationNumber(String confirmationNumber) {
        return bookingRepository.findByConfirmationNumber(confirmationNumber);
    }

    public List<Booking> findByUserId(Long userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public BigDecimal calculateTotal(List<BookingItem> items) {
        return items.stream()
                .map(BookingItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public Booking updateStatus(Long bookingId, BookingStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // Validate status transition
        if (!isValidTransition(booking.getStatus(), newStatus)) {
            throw new IllegalStateException(String.format(
                "Cannot transition booking from %s to %s", 
                booking.getStatus(), newStatus));
        }

        booking.setStatus(newStatus);
        return bookingRepository.save(booking);
    }

    private boolean isValidTransition(BookingStatus current, BookingStatus target) {
        return switch (current) {
            case PENDING -> target == BookingStatus.CONFIRMED || 
                           target == BookingStatus.CANCELLED || 
                           target == BookingStatus.EXPIRED ||
                           target == BookingStatus.PAYMENT_PROCESSING;
            case PAYMENT_PROCESSING -> target == BookingStatus.CONFIRMED ||
                           target == BookingStatus.CANCELLED ||
                           target == BookingStatus.FAILED;
            case CONFIRMED -> target == BookingStatus.CANCELLED ||
                             target == BookingStatus.REFUNDED;
            case CANCELLED, EXPIRED, REFUNDED, FAILED -> false;
        };
    }
}
