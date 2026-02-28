package com.ticket.common.kafka;

import com.ticket.common.entity.ProcessedEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Helper for implementing idempotent Kafka consumers.
 */
@Service
public class IdempotentConsumer {
    
    private static final Logger log = LoggerFactory.getLogger(IdempotentConsumer.class);
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Check if event was already processed.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public boolean isProcessed(String eventId, String consumerGroup) {
        Long count = entityManager.createQuery(
                "SELECT COUNT(pe) FROM ProcessedEvent pe " +
                "WHERE pe.eventId = :eventId AND pe.consumerGroup = :consumerGroup", 
                Long.class)
                .setParameter("eventId", eventId)
                .setParameter("consumerGroup", consumerGroup)
                .getSingleResult();
        
        return count > 0;
    }
    
    /**
     * Mark event as processed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markAsProcessed(String eventId, String consumerGroup, String eventType) {
        try {
            // Double-check not already processed
            if (isProcessed(eventId, consumerGroup)) {
                log.debug("Event already processed: {}", eventId);
                return false;
            }
            
            ProcessedEvent processed = new ProcessedEvent(
                    UUID.randomUUID().toString(),
                    eventId,
                    consumerGroup,
                    eventType
            );
            
            entityManager.persist(processed);
            entityManager.flush();
            
            log.debug("Marked event as processed: {} for consumer {}", eventId, consumerGroup);
            return true;
            
        } catch (Exception e) {
            // Unique constraint violation means duplicate processing attempt
            log.debug("Event already being processed: {}", eventId);
            return false;
        }
    }
    
    /**
     * Process event idempotently - returns true if processing should proceed.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean tryProcess(String eventId, String consumerGroup, String eventType) {
        if (isProcessed(eventId, consumerGroup)) {
            log.info("Skipping duplicate event: {} for consumer {}", eventId, consumerGroup);
            return false;
        }
        return markAsProcessed(eventId, consumerGroup, eventType);
    }
    
    /**
     * Process event idempotently (without event type) - returns true if processing should proceed.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean tryProcess(String eventId, String consumerGroup) {
        return tryProcess(eventId, consumerGroup, "UNKNOWN");
    }
}
