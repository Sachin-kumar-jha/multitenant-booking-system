package com.ticket.payment.dto;

import com.ticket.payment.entity.Payment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class PaymentMapper {

    private PaymentMapper() {}

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getBookingId(),
            payment.getConfirmationNumber(),
            payment.getUserId(),
            payment.getEventId(),
            payment.getAmount(),
            payment.getFee(),
            payment.getTotalAmount(),
            payment.getCurrency(),
            payment.getStatus(),
            payment.getPaymentMethod(),
            payment.getTransactionReference(),
            payment.getFailureReason(),
            payment.getProcessedAt(),
            payment.getRefundAmount(),
            payment.getRefundStatus(),
            toLocalDateTime(payment.getCreatedAt())
        );
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? instant.atZone(ZoneId.systemDefault()).toLocalDateTime() : null;
    }
}
