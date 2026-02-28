package com.ticket.booking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import static com.ticket.common.kafka.KafkaTopics.*;

@Configuration
public class KafkaConfig {

    @Bean
    public NewTopic bookingRequestedTopic() {
        return TopicBuilder.name(BOOKING_REQUESTED)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookingConfirmedTopic() {
        return TopicBuilder.name(BOOKING_CONFIRMED)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic bookingCancelledTopic() {
        return TopicBuilder.name(BOOKING_CANCELLED)
                .partitions(6)
                .replicas(1)
                .build();
    }
}
