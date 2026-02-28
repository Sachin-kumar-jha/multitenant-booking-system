package com.ticket.notification.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.common.tenant.TenantContext;
import com.ticket.common.kafka.IdempotentConsumer;
import com.ticket.common.kafka.KafkaTopics;
import com.ticket.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class BookingEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(BookingEventConsumer.class);

    private final NotificationService notificationService;
    private final IdempotentConsumer idempotentConsumer;
    private final ObjectMapper objectMapper;

    public BookingEventConsumer(NotificationService notificationService,
                                IdempotentConsumer idempotentConsumer,
                                ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.idempotentConsumer = idempotentConsumer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CONFIRMED, groupId = "${spring.kafka.consumer.group-id}")
    public void onBookingConfirmed(@Payload String payload,
                                   @Header("X-Tenant-Id") String tenantId,
                                   @Header(name = "X-Event-Id", required = false) String eventId) {
        if (eventId != null) {
            try {
                TenantContext.setCurrentTenant(tenantId);
                
                if (!idempotentConsumer.tryProcess(eventId, KafkaTopics.BOOKING_CONFIRMED)) {
                    logger.info("Duplicate booking confirmed event: {}", eventId);
                    return;
                }

                logger.info("Processing booking confirmed event: {} for tenant: {}", eventId, tenantId);
                
                Map<String, Object> data = objectMapper.readValue(payload, 
                        new TypeReference<Map<String, Object>>() {});

                Long userId = getLong(data, "userId");
                String userEmail = getString(data, "userEmail");
                String userName = getString(data, "userName");
                Long bookingId = getLong(data, "bookingId");
                String confirmationNumber = getString(data, "confirmationNumber");
                String eventName = getString(data, "eventName");
                LocalDateTime eventDate = data.get("eventDate") != null 
                        ? LocalDateTime.parse(data.get("eventDate").toString()) : null;
                String venue = getString(data, "venue");
                List<Long> seatIds = data.get("seatIds") != null 
                        ? objectMapper.convertValue(data.get("seatIds"), new TypeReference<List<Long>>() {}) 
                        : List.of();
                BigDecimal amount = data.get("amount") != null 
                        ? new BigDecimal(data.get("amount").toString()) : BigDecimal.ZERO;
                String paymentReference = getString(data, "paymentReference");

                notificationService.sendBookingConfirmation(userId, userEmail, userName,
                        bookingId, confirmationNumber, eventName, eventDate, venue,
                        seatIds, amount, paymentReference);

                logger.info("Successfully processed booking confirmed event: {}", eventId);
            } catch (Exception e) {
                logger.error("Error processing booking confirmed event: {}", e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @KafkaListener(topics = KafkaTopics.BOOKING_CANCELLED, groupId = "${spring.kafka.consumer.group-id}")
    public void onBookingCancelled(@Payload String payload,
                                   @Header("X-Tenant-Id") String tenantId,
                                   @Header(name = "X-Event-Id", required = false) String eventId) {
        if (eventId != null) {
            try {
                TenantContext.setCurrentTenant(tenantId);

                if (!idempotentConsumer.tryProcess(eventId, KafkaTopics.BOOKING_CANCELLED)) {
                    logger.info("Duplicate booking cancelled event: {}", eventId);
                    return;
                }

                logger.info("Processing booking cancelled event: {} for tenant: {}", eventId, tenantId);
                
                Map<String, Object> data = objectMapper.readValue(payload, 
                        new TypeReference<Map<String, Object>>() {});

                Long userId = getLong(data, "userId");
                String userEmail = getString(data, "userEmail");
                Long bookingId = getLong(data, "bookingId");
                String confirmationNumber = getString(data, "confirmationNumber");
                String reason = getString(data, "reason");

                notificationService.sendBookingCancellation(userId, userEmail, bookingId, 
                        confirmationNumber, reason != null ? reason : "Booking was cancelled");

                logger.info("Successfully processed booking cancelled event: {}", eventId);
            } catch (Exception e) {
                logger.error("Error processing booking cancelled event: {}", e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
}
