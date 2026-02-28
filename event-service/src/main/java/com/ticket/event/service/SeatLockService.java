package com.ticket.event.service;

import com.ticket.common.exception.BusinessException;
import com.ticket.common.exception.ResourceNotFoundException;
import com.ticket.common.redis.RedisLockService;
import com.ticket.common.tenant.TenantContext;
import com.ticket.event.dto.LockSeatsRequest;
import com.ticket.event.dto.SeatLockResponse;
import com.ticket.event.entity.Event;
import com.ticket.event.entity.EventStatus;
import com.ticket.event.entity.Seat;
import com.ticket.event.entity.SeatStatus;
import com.ticket.event.repository.EventRepository;
import com.ticket.event.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class SeatLockService {

    private static final Logger logger = LoggerFactory.getLogger(SeatLockService.class);
    private static final String SEAT_LOCK_KEY_PREFIX = "seat:lock:";
    private static final String USER_LOCKS_KEY_PREFIX = "user:locks:";

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository;
    private final RedisLockService redisLockService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${seat.lock.ttl-seconds:600}")
    private int lockTtlSeconds;

    @Value("${seat.lock.max-seats-per-booking:10}")
    private int maxSeatsPerBooking;

    public SeatLockService(SeatRepository seatRepository,
                          EventRepository eventRepository,
                          RedisLockService redisLockService,
                          RedisTemplate<String, Object> redisTemplate) {
        this.seatRepository = seatRepository;
        this.eventRepository = eventRepository;
        this.redisLockService = redisLockService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public SeatLockResponse lockSeats(LockSeatsRequest request, Long userId) {
        String tenantId = TenantContext.getTenantId();
        logger.info("User {} attempting to lock {} seats for event {}", 
                   userId, request.seatIds().size(), request.eventId());

        // Validate request
        if (request.seatIds().size() > maxSeatsPerBooking) {
            throw new BusinessException("Cannot lock more than " + maxSeatsPerBooking + " seats at once");
        }

        // Get event and validate
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", request.eventId()));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessException("Event is not available for booking");
        }

        if (!event.isSalesOpen()) {
            throw new BusinessException("Ticket sales are not open for this event");
        }

        // Check if user already has locks on this event
        String userLockKey = getUserLockKey(tenantId, userId, request.eventId());
        String existingLockId = (String) redisTemplate.opsForValue().get(userLockKey);
        if (existingLockId != null) {
            // Release existing locks first
            releaseUserLocks(userId, request.eventId());
        }

        // Get distributed lock for the operation
        String distributedLockKey = "lock:seats:" + tenantId + ":" + request.eventId();
        boolean acquired = redisLockService.tryLock(distributedLockKey, Duration.ofSeconds(30));
        
        if (!acquired) {
            throw new BusinessException("High demand for this event. Please try again.");
        }

        try {
            return performSeatLocking(request, userId, event, tenantId);
        } finally {
            redisLockService.unlock(distributedLockKey);
        }
    }

    private SeatLockResponse performSeatLocking(LockSeatsRequest request, Long userId, 
                                                 Event event, String tenantId) {
        // Lock seats in database with pessimistic locking
        List<Seat> seats = seatRepository.findAllByIdWithLock(request.seatIds());

        // Validate all seats belong to the event and are available
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (Seat seat : seats) {
            if (!seat.getEvent().getId().equals(request.eventId())) {
                throw new BusinessException("Seat " + seat.getId() + " does not belong to this event");
            }
            if (!seat.isAvailable()) {
                throw new BusinessException("Seat " + seat.getSeatIdentifier() + " is not available");
            }
            totalPrice = totalPrice.add(seat.getFinalPrice());
        }

        // Check max tickets per user for the event
        long userBookedSeats = countUserBookedSeatsForEvent(userId, request.eventId());
        if (userBookedSeats + seats.size() > event.getMaxTicketsPerUser()) {
            throw new BusinessException("You can only book up to " + event.getMaxTicketsPerUser() + 
                                       " tickets for this event. You already have " + userBookedSeats);
        }

        // Generate lock ID
        String lockId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(lockTtlSeconds);

        // Lock seats in database
        for (Seat seat : seats) {
            seat.lockForUser(userId, expiresAt);
        }
        seatRepository.saveAll(seats);

        // Store lock in Redis for quick lookups
        storeLockInRedis(lockId, tenantId, userId, request, expiresAt);

        logger.info("Locked {} seats for user {} with lock ID {}. Expires at {}", 
                   seats.size(), userId, lockId, expiresAt);

        return new SeatLockResponse(
            lockId,
            request.eventId(),
            userId,
            request.seatIds(),
            totalPrice,
            expiresAt,
            lockTtlSeconds
        );
    }

    private void storeLockInRedis(String lockId, String tenantId, Long userId, 
                                  LockSeatsRequest request, LocalDateTime expiresAt) {
        // Store lock details
        String lockKey = getSeatLockKey(tenantId, lockId);
        SeatLockData lockData = new SeatLockData(
            lockId, tenantId, request.eventId(), userId, 
            request.seatIds(), expiresAt
        );
        redisTemplate.opsForValue().set(lockKey, lockData, lockTtlSeconds, TimeUnit.SECONDS);

        // Store reference for user
        String userLockKey = getUserLockKey(tenantId, userId, request.eventId());
        redisTemplate.opsForValue().set(userLockKey, lockId, lockTtlSeconds, TimeUnit.SECONDS);

        // Store reference for each seat
        for (Long seatId : request.seatIds()) {
            String seatKey = SEAT_LOCK_KEY_PREFIX + tenantId + ":" + seatId;
            redisTemplate.opsForValue().set(seatKey, lockId, lockTtlSeconds, TimeUnit.SECONDS);
        }
    }

    @Transactional
    public void releaseUserLocks(Long userId, Long eventId) {
        String tenantId = TenantContext.getTenantId();
        String userLockKey = getUserLockKey(tenantId, userId, eventId);
        String lockId = (String) redisTemplate.opsForValue().get(userLockKey);

        if (lockId != null) {
            releaseLock(lockId, userId);
        }

        // Also release from database
        List<Seat> lockedSeats = seatRepository.findLockedSeatsByUserAndEvent(userId, eventId);
        for (Seat seat : lockedSeats) {
            seat.unlock();
        }
        seatRepository.saveAll(lockedSeats);
    }

    @Transactional
    public void releaseLock(String lockId, Long userId) {
        String tenantId = TenantContext.getTenantId();
        String lockKey = getSeatLockKey(tenantId, lockId);
        SeatLockData lockData = (SeatLockData) redisTemplate.opsForValue().get(lockKey);

        if (lockData == null) {
            logger.warn("Lock {} not found or already expired", lockId);
            return;
        }

        if (!lockData.userId().equals(userId)) {
            throw new BusinessException("You don't have permission to release this lock");
        }

        // Remove lock data
        redisTemplate.delete(lockKey);

        // Remove user reference
        String userLockKey = getUserLockKey(tenantId, userId, lockData.eventId());
        redisTemplate.delete(userLockKey);

        // Remove seat references
        for (Long seatId : lockData.seatIds()) {
            String seatKey = SEAT_LOCK_KEY_PREFIX + tenantId + ":" + seatId;
            redisTemplate.delete(seatKey);
        }

        // Release seats in database
        List<Seat> seats = seatRepository.findAllById(lockData.seatIds());
        for (Seat seat : seats) {
            if (seat.isLockedByUser(userId)) {
                seat.unlock();
            }
        }
        seatRepository.saveAll(seats);

        logger.info("Released lock {} for user {}", lockId, userId);
    }

    public SeatLockData getLockData(String lockId) {
        String tenantId = TenantContext.getTenantId();
        String lockKey = getSeatLockKey(tenantId, lockId);
        return (SeatLockData) redisTemplate.opsForValue().get(lockKey);
    }

    public boolean validateLock(String lockId, Long userId, List<Long> seatIds) {
        SeatLockData lockData = getLockData(lockId);
        if (lockData == null) {
            return false;
        }
        if (!lockData.userId().equals(userId)) {
            return false;
        }
        if (LocalDateTime.now().isAfter(lockData.expiresAt())) {
            return false;
        }
        return lockData.seatIds().containsAll(seatIds);
    }

    public boolean isSeatLocked(Long seatId) {
        String tenantId = TenantContext.getTenantId();
        String seatKey = SEAT_LOCK_KEY_PREFIX + tenantId + ":" + seatId;
        return redisTemplate.hasKey(seatKey);
    }

    private long countUserBookedSeatsForEvent(Long userId, Long eventId) {
        // Count seats that are either locked by user or booked by user
        List<Seat> lockedSeats = seatRepository.findLockedSeatsByUserAndEvent(userId, eventId);
        // For now, just return locked seats count
        // In a real system, you'd also check booking history
        return lockedSeats.size();
    }

    private String getSeatLockKey(String tenantId, String lockId) {
        return SEAT_LOCK_KEY_PREFIX + tenantId + ":lock:" + lockId;
    }

    private String getUserLockKey(String tenantId, Long userId, Long eventId) {
        return USER_LOCKS_KEY_PREFIX + tenantId + ":" + userId + ":" + eventId;
    }

    // Scheduled task to clean up expired locks
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void cleanupExpiredLocks() {
        int released = seatRepository.releaseExpiredLocks(LocalDateTime.now());
        if (released > 0) {
            logger.info("Released {} expired seat locks", released);
        }
    }

    // Record for storing lock data in Redis
    public record SeatLockData(
        String lockId,
        String tenantId,
        Long eventId,
        Long userId,
        List<Long> seatIds,
        LocalDateTime expiresAt
    ) implements java.io.Serializable {}
}
