package com.medikit.inventory.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.event.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class InventoryConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryConsumer.class);
    private static final String STOCK_CACHE_KEY_PREFIX = "medikit:inventory:stock:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public InventoryConsumer(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.PRODUCT_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onProductUpdated(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            UUID productId = UUID.fromString(node.get("productId").asText());
            Set<String> keys = redisTemplate.keys(STOCK_CACHE_KEY_PREFIX + productId + ":*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Evicted {} cached stock entries for product {}", keys.size(), productId);
            }
        } catch (Exception e) {
            log.warn("Failed to process product updated event: {}", e.getMessage());
        }
    }
}
