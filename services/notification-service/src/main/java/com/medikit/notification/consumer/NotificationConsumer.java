package com.medikit.notification.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.event.Topics;
import com.medikit.notification.config.NotificationProperties;
import com.medikit.notification.model.Notification;
import com.medikit.notification.model.NotificationStatus;
import com.medikit.notification.model.NotificationType;
import com.medikit.notification.service.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class NotificationConsumer {

    private static final String DEDUP_KEY_PREFIX = "medikit:notification:dedup:";

    private final NotificationSender notificationSender;
    private final StringRedisTemplate redisTemplate;
    private final NotificationProperties properties;
    private final ObjectMapper objectMapper;

    public NotificationConsumer(NotificationSender notificationSender,
                                StringRedisTemplate redisTemplate,
                                NotificationProperties properties,
                                ObjectMapper objectMapper) {
        this.notificationSender = notificationSender;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {
            Topics.ORDER_CONFIRMED,
            Topics.ORDER_CANCELLED,
            Topics.ORDER_COMPLETED,
            Topics.PAYMENT_COMPLETED,
            Topics.DELIVERY_UPDATED,
            Topics.INVENTORY_RESERVATION_FAILED
    })
    public void onEvent(ConsumerRecord<String, String> record) {
        String topic = record.topic();
        try {
            JsonNode json = objectMapper.readTree(record.value());
            String eventId = extractEventId(json, record.key());
            if (!deduplicate(eventId)) {
                log.info("Duplicate event [{}] on topic [{}] skipped", eventId, topic);
                return;
            }
            Notification notification = buildNotification(topic, json, eventId);
            Notification result = notificationSender.send(notification);
            log.info("Processed [{}] event -> notification {} status={}",
                    topic, result.getId(), result.getStatus());
        } catch (Exception e) {
            log.error("Failed to process event on topic [{}]: {}", topic, record.value(), e);
        }
    }

    private Notification buildNotification(String topic, JsonNode json, String eventId) {
        String orderId = text(json, "orderId");
        String orderRef = orderId.isEmpty() ? eventId : orderId;

        NotificationType type;
        String subject;
        String body;
        switch (topic) {
            case Topics.ORDER_CONFIRMED:
                type = NotificationType.EMAIL;
                subject = "Order confirmed";
                body = String.format("Your order %s has been confirmed", orderRef);
                break;
            case Topics.ORDER_CANCELLED:
                type = NotificationType.SMS;
                subject = "Order cancelled";
                body = String.format("Your order %s has been cancelled", orderRef);
                break;
            case Topics.ORDER_COMPLETED:
                type = NotificationType.EMAIL;
                subject = "Order completed";
                body = String.format("Your order %s has been completed. Thank you for choosing Medikit!", orderRef);
                break;
            case Topics.PAYMENT_COMPLETED:
                type = NotificationType.EMAIL;
                subject = "Payment successful";
                String amount = text(json, "amount");
                body = amount.isEmpty()
                        ? String.format("Your payment for order %s was successful", orderRef)
                        : String.format("Your payment of %s for order %s was successful", amount, orderRef);
                break;
            case Topics.DELIVERY_UPDATED:
                type = NotificationType.PUSH;
                subject = "Delivery update";
                body = String.format("Your order %s delivery status: %s", orderRef, text(json, "status"));
                break;
            case Topics.INVENTORY_RESERVATION_FAILED:
                type = NotificationType.SMS;
                subject = "Order could not be processed";
                body = String.format("Sorry, the items in your order %s could not be reserved", orderRef);
                break;
            default:
                throw new IllegalArgumentException("Unsupported notification topic " + topic);
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("topic", topic);
        metadata.put("eventId", eventId);
        if (json.has("amount")) {
            metadata.put("amount", json.get("amount").asText());
        }

        String recipient = firstNonEmpty(json, "recipient", "email");
        if (recipient == null) {
            recipient = properties.getDefaultRecipient();
        }

        return Notification.builder()
                .id(UUID.randomUUID().toString())
                .type(type)
                .channel(type.name().toLowerCase())
                .recipient(recipient)
                .subject(subject)
                .body(body)
                .status(NotificationStatus.SENT)
                .timestamp(Instant.now())
                .metadata(metadata)
                .build();
    }

    private boolean deduplicate(String eventId) {
        String key = DEDUP_KEY_PREFIX + eventId;
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(properties.getDedupTtlSeconds()));
        return Boolean.TRUE.equals(first);
    }

    private String extractEventId(JsonNode json, String key) {
        String eventId = firstNonEmpty(json, "eventId", "id");
        if (eventId == null && key != null && !key.isEmpty()) {
            eventId = key;
        }
        if (eventId == null) {
            eventId = String.valueOf(json.hashCode());
        }
        return eventId;
    }

    private static String text(JsonNode json, String field) {
        JsonNode node = json.get(field);
        return node == null || node.isNull() ? "" : node.asText();
    }

    private static String firstNonEmpty(JsonNode json, String... fields) {
        for (String field : fields) {
            String value = text(json, field);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return null;
    }
}
