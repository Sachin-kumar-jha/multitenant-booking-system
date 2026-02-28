package com.ticket.event.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SeatLockResponse(
    String lockId,
    Long eventId,
    Long userId,
    List<Long> seatIds,
    BigDecimal totalPrice,
    LocalDateTime expiresAt,
    Integer ttlSeconds
) {}
