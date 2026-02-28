package com.ticket.common.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.common.event.BaseEvent;
import com.ticket.common.tenant.TenantContext;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka event publisher with tenant context propagation.
 */
@Service
public class EventPublisher {
    
    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    public EventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publish an event to the specified topic.
     * Uses tenantId as the partition key for ordering guarantees.
     */
    public <T extends BaseEvent> CompletableFuture<SendResult<String, String>> publish(
            String topic, T event) {
        
        try {
            // Ensure tenant and correlation IDs are set
            if (event.getTenantId() == null) {
                event.setTenantId(TenantContext.getCurrentTenant());
            }
            if (event.getCorrelationId() == null) {
                event.setCorrelationId(TenantContext.getCorrelationId());
            }
            
            String payload = objectMapper.writeValueAsString(event);
            
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    topic,
                    null, // partition (let Kafka decide based on key)
                    event.getTenantId(), // key for partitioning
                    payload
            );
            
            // Add headers for tracing
            record.headers()
                    .add(new RecordHeader("tenantId", 
                            event.getTenantId().getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("correlationId", 
                            event.getCorrelationId().getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("eventType", 
                            event.getEventType().getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("eventId", 
                            event.getEventId().getBytes(StandardCharsets.UTF_8)));
            
            log.info("Publishing event: topic={}, type={}, eventId={}, tenant={}", 
                    topic, event.getEventType(), event.getEventId(), event.getTenantId());
            
            return kafkaTemplate.send(record);
            
        } catch (Exception e) {
            log.error("Failed to publish event to topic {}: {}", topic, e.getMessage());
            throw new RuntimeException("Failed to publish event", e);
        }
    }
    
    /**
     * Publish an event to the specified topic with a custom partition key.
     */
    public <T extends BaseEvent> CompletableFuture<SendResult<String, String>> publish(
            String topic, String key, T event) {
        
        try {
            // Ensure tenant and correlation IDs are set
            if (event.getTenantId() == null) {
                event.setTenantId(TenantContext.getCurrentTenant());
            }
            if (event.getCorrelationId() == null) {
                event.setCorrelationId(TenantContext.getCorrelationId());
            }
            
            String payload = objectMapper.writeValueAsString(event);
            
            ProducerRecord<String, String> record = new ProducerRecord<>(
                    topic,
                    null, // partition (let Kafka decide based on key)
                    key, // custom key for partitioning
                    payload
            );
            
            // Add headers for tracing
            record.headers()
                    .add(new RecordHeader("tenantId", 
                            event.getTenantId().getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("correlationId", 
                            event.getCorrelationId().getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("eventType", 
                            event.getEventType().getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("eventId", 
                            event.getEventId().getBytes(StandardCharsets.UTF_8)));
            
            log.info("Publishing event: topic={}, key={}, type={}, eventId={}, tenant={}", 
                    topic, key, event.getEventType(), event.getEventId(), event.getTenantId());
            
            return kafkaTemplate.send(record);
            
        } catch (Exception e) {
            log.error("Failed to publish event to topic {}: {}", topic, e.getMessage());
            throw new RuntimeException("Failed to publish event", e);
        }
    }
    
    /**
     * Publish event synchronously, blocking until acknowledged.
     */
    public <T extends BaseEvent> void publishSync(String topic, T event) {
        try {
            publish(topic, event).get();
        } catch (Exception e) {
            log.error("Sync publish failed: {}", e.getMessage());
            throw new RuntimeException("Failed to publish event synchronously", e);
        }
    }
    
    /**
     * Publish to Dead Letter Queue.
     */
    public void publishToDLQ(String dlqTopic, String originalTopic, String payload, 
                              String reason, Exception error) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(dlqTopic, payload);
            
            record.headers()
                    .add(new RecordHeader("originalTopic", 
                            originalTopic.getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("failureReason", 
                            reason.getBytes(StandardCharsets.UTF_8)))
                    .add(new RecordHeader("errorMessage", 
                            (error != null ? error.getMessage() : "unknown")
                                    .getBytes(StandardCharsets.UTF_8)));
            
            kafkaTemplate.send(record);
            
            log.warn("Event sent to DLQ: topic={}, reason={}", dlqTopic, reason);
            
        } catch (Exception e) {
            log.error("Failed to send to DLQ: {}", e.getMessage());
        }
    }
}
