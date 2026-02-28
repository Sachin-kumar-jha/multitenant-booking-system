package com.ticket.booking.entity;

public enum BookingStatus {
    PENDING,        // Booking created, awaiting payment
    PAYMENT_PROCESSING, // Payment in progress
    CONFIRMED,      // Payment successful, booking confirmed
    CANCELLED,      // Booking cancelled by user
    EXPIRED,        // Payment timeout, booking expired
    REFUNDED,       // Booking cancelled and refunded
    FAILED          // Booking failed (payment failed, seats unavailable, etc.)
}
