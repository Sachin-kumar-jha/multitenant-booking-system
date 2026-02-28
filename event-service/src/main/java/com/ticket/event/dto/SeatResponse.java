package com.ticket.event.dto;

import com.ticket.event.entity.SeatStatus;
import com.ticket.event.entity.SeatType;
import java.math.BigDecimal;

public record SeatResponse(
    Long id,
    Long eventId,
    String section,
    String rowNumber,
    String seatNumber,
    SeatType type,
    SeatStatus status,
    BigDecimal price,
    BigDecimal priceMultiplier,
    BigDecimal finalPrice,
    String seatIdentifier,
    Boolean isAvailable
) {}
