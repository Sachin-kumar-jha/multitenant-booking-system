package com.ticket.booking.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for event data from Event Service
 */
public record EventData(
    Long id,
    String name,
    String description,
    String venue,
    LocalDateTime eventDate,
    Integer totalSeats,
    Integer availableSeats,
    BigDecimal basePrice,
    String status,
    Integer maxTicketsPerUser,
    Boolean isSalesOpen,
    Boolean isRefundable,
    Integer refundPercentage
) {}
