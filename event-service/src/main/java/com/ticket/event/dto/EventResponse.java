package com.ticket.event.dto;

import com.ticket.event.entity.EventCategory;
import com.ticket.event.entity.EventStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventResponse(
    Long id,
    String name,
    String description,
    String venue,
    String venueAddress,
    LocalDateTime eventDate,
    LocalDateTime doorsOpenTime,
    LocalDateTime endTime,
    Integer totalSeats,
    Integer availableSeats,
    BigDecimal basePrice,
    EventStatus status,
    EventCategory category,
    String imageUrl,
    Long organizerId,
    String organizerName,
    Integer maxTicketsPerUser,
    LocalDateTime salesStartDate,
    LocalDateTime salesEndDate,
    LocalDateTime cancellationDeadline,
    Boolean isRefundable,
    Integer refundPercentage,
    Boolean isSalesOpen,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
