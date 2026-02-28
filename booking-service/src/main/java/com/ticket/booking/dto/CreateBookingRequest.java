package com.ticket.booking.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateBookingRequest(
    @NotNull(message = "Event ID is required")
    Long eventId,

    @NotBlank(message = "Lock ID is required")
    String lockId,

    @NotEmpty(message = "At least one seat must be selected")
    @Size(max = 10, message = "Cannot book more than 10 seats at once")
    List<Long> seatIds,

    String promoCode
) {}
