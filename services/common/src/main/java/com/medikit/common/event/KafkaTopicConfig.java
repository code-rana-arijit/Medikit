package com.medikit.common.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "medikit.kafka.topics.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTopicConfig {

    private final int partitions;
    private final short replicas;

    public KafkaTopicConfig(
            @Value("${medikit.kafka.topics.partitions:12}") int partitions,
            @Value("${medikit.kafka.topics.replicas:1}") short replicas) {
        this.partitions = partitions;
        this.replicas = replicas;
    }

    @Bean
    public List<NewTopic> medikitTopics() {
        int order = Math.max(partitions, 24);
        int hot = Math.max(partitions, 12);
        return List.of(
                topic(Topics.ORDER_CREATED, order),
                topic(Topics.ORDER_CONFIRMED, order),
                topic(Topics.ORDER_CANCELLED, order),
                topic(Topics.ORDER_FAILED, order),
                topic(Topics.ORDER_COMPLETED, order),
                topic(Topics.PAYMENT_INITIATED, hot),
                topic(Topics.PAYMENT_COMPLETED, hot),
                topic(Topics.PAYMENT_FAILED, hot),
                topic(Topics.INVENTORY_RESERVED, order),
                topic(Topics.INVENTORY_RESERVATION_FAILED, order),
                topic(Topics.INVENTORY_RELEASED, order),
                topic(Topics.INVENTORY_DEDUCTED, order),
                topic(Topics.DELIVERY_ASSIGNED, hot),
                topic(Topics.DELIVERY_UPDATED, hot),
                topic(Topics.NOTIFICATION_SEND, hot),
                topic(Topics.PRODUCT_UPDATED, hot),
                topic(Topics.STOCK_UPDATED, hot)
        );
    }

    private NewTopic topic(String name, int partitions) {
        return TopicBuilder.name(name)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
