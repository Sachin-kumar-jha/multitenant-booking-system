package com.ticket.booking.entity;

public enum SagaStep {
    SEATS_LOCKED,
    PAYMENT_REQUESTED,
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
    SEATS_CONFIRMED,
    SEATS_RELEASED,
    NOTIFICATION_SENT
}
