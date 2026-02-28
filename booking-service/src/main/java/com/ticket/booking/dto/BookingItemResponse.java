package com.ticket.booking.dto;

import com.ticket.booking.entity.TicketStatus;
import java.math.BigDecimal;

public record BookingItemResponse(
    Long id,
    Long seatId,
    String seatSection,
    String seatRow,
    String seatNumber,
    String seatType,
    BigDecimal price,
    String ticketCode,
    TicketStatus ticketStatus,
    String seatIdentifier
) {}
