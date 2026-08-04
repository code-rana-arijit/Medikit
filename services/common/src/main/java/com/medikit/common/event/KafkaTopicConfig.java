package com.medikit.common.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "medikit.kafka.topics.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    @Bean
    public List<NewTopic> medikitTopics() {
        return List.of(
                topic(Topics.ORDER_CREATED, 12),
                topic(Topics.ORDER_CONFIRMED, 12),
                topic(Topics.ORDER_CANCELLED, 12),
                topic(Topics.ORDER_FAILED, 12),
                topic(Topics.ORDER_COMPLETED, 12),
                topic(Topics.PAYMENT_INITIATED, 6),
                topic(Topics.PAYMENT_COMPLETED, 6),
                topic(Topics.PAYMENT_FAILED, 6),
                topic(Topics.INVENTORY_RESERVED, 12),
                topic(Topics.INVENTORY_RESERVATION_FAILED, 12),
                topic(Topics.INVENTORY_RELEASED, 12),
                topic(Topics.INVENTORY_DEDUCTED, 12),
                topic(Topics.DELIVERY_ASSIGNED, 6),
                topic(Topics.DELIVERY_UPDATED, 6),
                topic(Topics.NOTIFICATION_SEND, 6),
                topic(Topics.PRODUCT_UPDATED, 6),
                topic(Topics.STOCK_UPDATED, 6)
        );
    }

    private NewTopic topic(String name, int partitions) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(1)
                .build();
    }
}
