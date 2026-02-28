package com.ticket.event.dto;

import com.ticket.event.entity.EventCategory;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateEventRequest(
    @NotBlank(message = "Event name is required")
    @Size(max = 255, message = "Event name cannot exceed 255 characters")
    String name,

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    String description,

    @NotBlank(message = "Venue is required")
    @Size(max = 255, message = "Venue name cannot exceed 255 characters")
    String venue,

    @Size(max = 500, message = "Venue address cannot exceed 500 characters")
    String venueAddress,

    @NotNull(message = "Event date is required")
    @Future(message = "Event date must be in the future")
    LocalDateTime eventDate,

    LocalDateTime doorsOpenTime,

    LocalDateTime endTime,

    @NotNull(message = "Total seats is required")
    @Min(value = 1, message = "Total seats must be at least 1")
    @Max(value = 100000, message = "Total seats cannot exceed 100,000")
    Integer totalSeats,

    @NotNull(message = "Base price is required")
    @DecimalMin(value = "0.0", message = "Base price cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid price format")
    BigDecimal basePrice,

    EventCategory category,

    String imageUrl,

    @Min(value = 1, message = "Max tickets per user must be at least 1")
    @Max(value = 50, message = "Max tickets per user cannot exceed 50")
    Integer maxTicketsPerUser,

    LocalDateTime salesStartDate,

    LocalDateTime salesEndDate,

    LocalDateTime cancellationDeadline,

    Boolean isRefundable,

    @Min(value = 0, message = "Refund percentage cannot be negative")
    @Max(value = 100, message = "Refund percentage cannot exceed 100")
    Integer refundPercentage,

    List<SeatSectionConfig> seatSections
) {
    public CreateEventRequest {
        if (maxTicketsPerUser == null) maxTicketsPerUser = 10;
        if (isRefundable == null) isRefundable = true;
        if (refundPercentage == null) refundPercentage = 100;
    }
}
