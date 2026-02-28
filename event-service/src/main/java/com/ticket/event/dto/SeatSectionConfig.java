package com.ticket.event.dto;

import com.ticket.event.entity.SeatType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record SeatSectionConfig(
    @NotBlank(message = "Section name is required")
    String section,

    @NotNull(message = "Number of rows is required")
    @Min(value = 1, message = "Must have at least 1 row")
    @Max(value = 100, message = "Cannot exceed 100 rows")
    Integer rows,

    @NotNull(message = "Seats per row is required")
    @Min(value = 1, message = "Must have at least 1 seat per row")
    @Max(value = 100, message = "Cannot exceed 100 seats per row")
    Integer seatsPerRow,

    @NotNull(message = "Seat type is required")
    SeatType seatType,

    @NotNull(message = "Price multiplier is required")
    @DecimalMin(value = "0.1", message = "Price multiplier must be at least 0.1")
    @DecimalMax(value = "10.0", message = "Price multiplier cannot exceed 10.0")
    BigDecimal priceMultiplier
) {}
