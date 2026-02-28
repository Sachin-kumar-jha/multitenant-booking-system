package com.ticket.tenant.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static com.ticket.common.kafka.KafkaTopics.*;

/**
 * Kafka topic configuration for Tenant Service.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic tenantCreatedTopic() {
        return TopicBuilder.name(TENANT_CREATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tenantUpdatedTopic() {
        return TopicBuilder.name(TENANT_UPDATED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic tenantSuspendedTopic() {
        return TopicBuilder.name(TENANT_SUSPENDED)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
