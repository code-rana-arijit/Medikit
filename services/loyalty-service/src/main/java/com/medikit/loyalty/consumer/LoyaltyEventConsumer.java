package com.medikit.loyalty.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.event.Topics;
import com.medikit.loyalty.service.LoyaltyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class LoyaltyEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LoyaltyEventConsumer.class);

    private final LoyaltyService loyaltyService;
    private final ObjectMapper objectMapper;

    public LoyaltyEventConsumer(LoyaltyService loyaltyService, ObjectMapper objectMapper) {
        this.loyaltyService = loyaltyService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.ORDER_CONFIRMED, groupId = "loyalty-service")
    public void onOrderConfirmed(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            UUID orderId = UUID.fromString(node.path("orderId").asText());
            UUID userId = UUID.fromString(node.path("userId").asText());
            BigDecimal total = new BigDecimal(node.path("total").asText());

            loyaltyService.awardPoints(userId, orderId, total);
            log.info("Awarded loyalty points for order {} user {}", orderId, userId);
        } catch (Exception e) {
            log.error("Failed to award loyalty points", e);
        }
    }
}
