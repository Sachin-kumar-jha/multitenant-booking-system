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
import java.util.Map;

@Component
public class PaymentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final NotificationService notificationService;
    private final IdempotentConsumer idempotentConsumer;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(NotificationService notificationService,
                               IdempotentConsumer idempotentConsumer,
                               ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.idempotentConsumer = idempotentConsumer;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_COMPLETED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentCompleted(@Payload String payload,
                                   @Header("X-Tenant-Id") String tenantId,
                                   @Header(name = "X-Event-Id", required = false) String eventId) {
        if (eventId != null) {
            try {
                TenantContext.setCurrentTenant(tenantId);

                if (!idempotentConsumer.tryProcess(eventId, KafkaTopics.PAYMENT_COMPLETED + "_notification")) {
                    logger.info("Duplicate payment completed event for notification: {}", eventId);
                    return;
                }

                logger.info("Processing payment completed event: {} for tenant: {}", eventId, tenantId);
                
                Map<String, Object> data = objectMapper.readValue(payload, 
                        new TypeReference<Map<String, Object>>() {});

                Long userId = getLong(data, "userId");
                String userEmail = getString(data, "userEmail");
                Long paymentId = getLong(data, "paymentId");
                BigDecimal amount = data.get("amount") != null 
                        ? new BigDecimal(data.get("amount").toString()) : BigDecimal.ZERO;
                String transactionReference = getString(data, "transactionReference");

                if (userEmail != null) {
                    notificationService.sendPaymentConfirmation(userId, userEmail, paymentId, 
                            amount, transactionReference);
                }

                logger.info("Successfully processed payment completed event: {}", eventId);
            } catch (Exception e) {
                logger.error("Error processing payment completed event: {}", e.getMessage(), e);
            } finally {
                TenantContext.clear();
            }
        }
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_FAILED, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentFailed(@Payload String payload,
                                @Header("X-Tenant-Id") String tenantId,
                                @Header(name = "X-Event-Id", required = false) String eventId) {
        if (eventId != null) {
            try {
                TenantContext.setCurrentTenant(tenantId);

                if (!idempotentConsumer.tryProcess(eventId, KafkaTopics.PAYMENT_FAILED + "_notification")) {
                    logger.info("Duplicate payment failed event for notification: {}", eventId);
                    return;
                }

                logger.info("Processing payment failed event: {} for tenant: {}", eventId, tenantId);
                
                Map<String, Object> data = objectMapper.readValue(payload, 
                        new TypeReference<Map<String, Object>>() {});

                Long userId = getLong(data, "userId");
                String userEmail = getString(data, "userEmail");
                Long bookingId = getLong(data, "bookingId");
                String reason = getString(data, "reason");

                if (userEmail != null) {
                    notificationService.sendPaymentFailed(userId, userEmail, bookingId, 
                            reason != null ? reason : "Payment processing failed");
                }

                logger.info("Successfully processed payment failed event: {}", eventId);
            } catch (Exception e) {
                logger.error("Error processing payment failed event: {}", e.getMessage(), e);
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
