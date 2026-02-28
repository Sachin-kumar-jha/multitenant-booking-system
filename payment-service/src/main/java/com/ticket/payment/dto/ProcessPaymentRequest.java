package com.ticket.payment.dto;

import com.ticket.payment.entity.PaymentMethod;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProcessPaymentRequest(
    @NotNull(message = "Booking ID is required")
    Long bookingId,

    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod,

    // Card details (for card payments)
    String cardNumber,
    String cardHolderName,
    String expiryMonth,
    String expiryYear,
    String cvv,

    // For other payment methods
    String paymentToken,

    // Billing address
    String billingAddress,
    String billingCity,
    String billingCountry,
    String billingZip
) {}
