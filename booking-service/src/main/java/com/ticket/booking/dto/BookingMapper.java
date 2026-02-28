package com.ticket.booking.dto;

import com.ticket.booking.entity.Booking;
import com.ticket.booking.entity.BookingItem;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class BookingMapper {

    private BookingMapper() {}
    
    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }

    public static BookingResponse toResponse(Booking booking) {
        List<BookingItemResponse> items = booking.getItems().stream()
                .map(BookingMapper::toItemResponse)
                .toList();

        return new BookingResponse(
            booking.getId(),
            booking.getConfirmationNumber(),
            booking.getUserId(),
            booking.getUserEmail(),
            booking.getUserName(),
            booking.getEventId(),
            booking.getEventName(),
            booking.getEventDate(),
            booking.getVenue(),
            booking.getStatus(),
            booking.getTotalAmount(),
            booking.getDiscountAmount(),
            booking.getFinalAmount(),
            booking.getPaymentReference(),
            booking.getExpiresAt(),
            items,
            booking.getTicketCount(),
            booking.getCancelledAt(),
            booking.getCancellationReason(),
            booking.getRefundAmount(),
            booking.getRefundStatus(),
            toLocalDateTime(booking.getCreatedAt()),
            toLocalDateTime(booking.getUpdatedAt())
        );
    }

    public static BookingItemResponse toItemResponse(BookingItem item) {
        return new BookingItemResponse(
            item.getId(),
            item.getSeatId(),
            item.getSeatSection(),
            item.getSeatRow(),
            item.getSeatNumber(),
            item.getSeatType(),
            item.getPrice(),
            item.getTicketCode(),
            item.getTicketStatus(),
            item.getSeatIdentifier()
        );
    }
}
