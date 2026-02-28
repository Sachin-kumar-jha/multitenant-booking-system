package com.ticket.booking.dto;

import java.math.BigDecimal;

/**
 * DTO for seat data from Event Service
 */
public record SeatData(
    Long id,
    Long eventId,
    String section,
    String rowNumber,
    String seatNumber,
    String type,
    String status,
    BigDecimal price,
    BigDecimal priceMultiplier,
    BigDecimal finalPrice,
    String seatIdentifier,
    Boolean isAvailable
) {}
