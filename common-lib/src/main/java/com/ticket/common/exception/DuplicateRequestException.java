package com.ticket.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when duplicate request is detected.
 */
public class DuplicateRequestException extends BaseException {
    
    public DuplicateRequestException(String idempotencyKey) {
        super("Duplicate request detected with key: " + idempotencyKey, 
              HttpStatus.CONFLICT, "DUPLICATE_REQUEST");
    }
}
