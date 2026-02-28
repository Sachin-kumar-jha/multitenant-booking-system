package com.ticket.event.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static com.ticket.common.kafka.KafkaTopics.*;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic eventCreatedTopic() {
        return TopicBuilder.name(EVENT_CREATED)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic eventCancelledTopic() {
        return TopicBuilder.name(EVENT_CANCELLED)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
