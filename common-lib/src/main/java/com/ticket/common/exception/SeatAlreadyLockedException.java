package com.ticket.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a seat is already locked by another user.
 */
public class SeatAlreadyLockedException extends BaseException {
    
    public SeatAlreadyLockedException(String seatId) {
        super("Seat is already locked by another user: " + seatId, 
              HttpStatus.CONFLICT, "SEAT_ALREADY_LOCKED");
    }
}
