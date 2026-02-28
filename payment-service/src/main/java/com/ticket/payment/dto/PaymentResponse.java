package com.ticket.payment.dto;

import com.ticket.payment.entity.PaymentMethod;
import com.ticket.payment.entity.PaymentStatus;
import com.ticket.payment.entity.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
    Long id,
    Long bookingId,
    String confirmationNumber,
    Long userId,
    Long eventId,
    BigDecimal amount,
    BigDecimal fee,
    BigDecimal totalAmount,
    String currency,
    PaymentStatus status,
    PaymentMethod paymentMethod,
    String transactionReference,
    String failureReason,
    LocalDateTime processedAt,
    BigDecimal refundAmount,
    RefundStatus refundStatus,
    LocalDateTime createdAt
) {}
