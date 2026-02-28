package com.ticket.event.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record LockSeatsRequest(
    @NotNull(message = "Event ID is required")
    Long eventId,

    @NotEmpty(message = "At least one seat must be selected")
    @Size(max = 10, message = "Cannot lock more than 10 seats at once")
    List<Long> seatIds
) {}
