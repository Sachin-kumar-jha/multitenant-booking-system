package com.ticket.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base exception for all application exceptions.
 */
public abstract class BaseException extends RuntimeException {
    
    private final HttpStatus status;
    private final String errorCode;
    
    protected BaseException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
    
    protected BaseException(String message, Throwable cause, HttpStatus status, String errorCode) {
        super(message, cause);
        this.status = status;
        this.errorCode = errorCode;
    }
    
    public HttpStatus getStatus() {
        return status;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
