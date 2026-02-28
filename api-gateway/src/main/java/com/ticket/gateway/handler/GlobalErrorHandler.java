package com.ticket.gateway.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
@Order(-2)
public class GlobalErrorHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalErrorHandler.class);

    public GlobalErrorHandler(ErrorAttributes errorAttributes,
                              WebProperties webProperties,
                              ApplicationContext applicationContext,
                              ServerCodecConfigurer configurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        this.setMessageWriters(configurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorPropertiesMap = getErrorAttributes(request, 
            ErrorAttributeOptions.defaults());
        
        Throwable error = getError(request);
        HttpStatus status = determineHttpStatus(error, errorPropertiesMap);
        
        logger.error("Gateway error on path {}: {}", 
            request.path(), error.getMessage(), error);

        Map<String, Object> response = new HashMap<>();
        response.put("error", status.getReasonPhrase());
        response.put("message", getUserFriendlyMessage(error, status));
        response.put("path", request.path());
        response.put("timestamp", System.currentTimeMillis());

        return ServerResponse
            .status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(response));
    }

    private HttpStatus determineHttpStatus(Throwable error, Map<String, Object> errorProperties) {
        Integer statusCode = (Integer) errorProperties.get("status");
        if (statusCode != null) {
            try {
                return HttpStatus.valueOf(statusCode);
            } catch (IllegalArgumentException e) {
                return HttpStatus.INTERNAL_SERVER_ERROR;
            }
        }
        
        // Map common exceptions to status codes
        String errorClass = error.getClass().getSimpleName();
        return switch (errorClass) {
            case "ConnectException", "WebClientRequestException" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "TimeoutException" -> HttpStatus.GATEWAY_TIMEOUT;
            case "UnauthorizedException" -> HttpStatus.UNAUTHORIZED;
            case "ForbiddenException" -> HttpStatus.FORBIDDEN;
            case "NotFoundException" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private String getUserFriendlyMessage(Throwable error, HttpStatus status) {
        return switch (status) {
            case SERVICE_UNAVAILABLE -> "Service is temporarily unavailable. Please try again later.";
            case GATEWAY_TIMEOUT -> "Request timed out. Please try again.";
            case UNAUTHORIZED -> "Authentication required.";
            case FORBIDDEN -> "Access denied.";
            case NOT_FOUND -> "Resource not found.";
            case TOO_MANY_REQUESTS -> "Rate limit exceeded. Please slow down.";
            default -> "An unexpected error occurred. Please try again later.";
        };
    }
}
