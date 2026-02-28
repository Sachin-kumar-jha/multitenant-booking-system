package com.ticket.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown for booking-related errors.
 */
public class BookingException extends BaseException {
    
    public BookingException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BOOKING_ERROR");
    }
    
    public BookingException(String message, String errorCode) {
        super(message, HttpStatus.BAD_REQUEST, errorCode);
    }
    
    public BookingException(String message, HttpStatus status, String errorCode) {
        super(message, status, errorCode);
    }
}
