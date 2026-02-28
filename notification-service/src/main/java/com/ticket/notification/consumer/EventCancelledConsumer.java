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

import java.util.Map;

@Component
public class EventCancelledConsumer {

    private static final Logger logger = LoggerFactory.getLogger(EventCancelledConsumer.class);

    private final NotificationService notificationService;
    private final IdempotentConsumer idempotentConsumer;
    private final ObjectMapper objectMapper;

    public EventCancelledConsumer(NotificationService notificationService,
                                  IdempotentConsumer idempotentConsumer,
                                  ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.idempotentConsumer = idempotentConsumer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.EVENT_CANCELLED, groupId = "${spring.kafka.consumer.group-id}")
    public void onEventCancelled(@Payload String payload,
                                 @Header("X-Tenant-Id") String tenantId,
                                 @Header(name = "X-Event-Id", required = false) String eventId) {
        if (eventId != null) {
            try {
                TenantContext.setCurrentTenant(tenantId);

                if (!idempotentConsumer.tryProcess(eventId, KafkaTopics.EVENT_CANCELLED)) {
                    logger.info("Duplicate event cancelled event: {}", eventId);
                    return;
                }

                logger.info("Processing event cancelled event: {} for tenant: {}", eventId, tenantId);
                
                Map<String, Object> data = objectMapper.readValue(payload, 
                        new TypeReference<Map<String, Object>>() {});

                // The event cancelled message contains event details
                // In production, we would need to lookup all affected bookings and notify each user
                Long eventEntityId = getLong(data, "eventId");
                String eventName = getString(data, "eventName");
                String reason = getString(data, "reason");

                // For affected users notification, we would need to:
                // 1. Query booking service for all bookings for this event
                // 2. Send notification to each affected user
                // This would typically be handled by the booking service which has the user information
                
                logger.info("Event {} ({}) has been cancelled. Reason: {}", 
                        eventEntityId, eventName, reason);
                logger.info("Affected users would be notified via booking service cascading events");

                logger.info("Successfully processed event cancelled: {}", eventId);
            } catch (Exception e) {
                logger.error("Error processing event cancelled: {}", e.getMessage(), e);
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
