package com.ticket.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a resource is not found.
 */
public class ResourceNotFoundException extends BaseException {
    
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(resourceType + " not found: " + resourceId, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
    
    public ResourceNotFoundException(String resourceType, String fieldName, Object fieldValue) {
        super(resourceType + " not found with " + fieldName + ": " + fieldValue, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
