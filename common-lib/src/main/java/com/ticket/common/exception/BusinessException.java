package com.ticket.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Business logic exception for domain-related errors.
 */
public class BusinessException extends BaseException {
    
    public BusinessException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "BUSINESS_ERROR");
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_REQUEST, "BUSINESS_ERROR");
    }
    
    public BusinessException(String message, HttpStatus status) {
        super(message, status, "BUSINESS_ERROR");
    }
}
