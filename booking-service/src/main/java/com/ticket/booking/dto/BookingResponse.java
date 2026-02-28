package com.ticket.booking.dto;

import com.ticket.booking.entity.BookingStatus;
import com.ticket.booking.entity.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BookingResponse(
    Long id,
    String confirmationNumber,
    Long userId,
    String userEmail,
    String userName,
    Long eventId,
    String eventName,
    LocalDateTime eventDate,
    String venue,
    BookingStatus status,
    BigDecimal totalAmount,
    BigDecimal discountAmount,
    BigDecimal finalAmount,
    String paymentReference,
    LocalDateTime expiresAt,
    List<BookingItemResponse> items,
    Integer ticketCount,
    LocalDateTime cancelledAt,
    String cancellationReason,
    BigDecimal refundAmount,
    RefundStatus refundStatus,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
