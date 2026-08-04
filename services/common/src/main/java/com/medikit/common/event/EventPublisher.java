package com.medikit.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publish(String topic, Object payload) {
        try {
            String message = payload instanceof String
                    ? (String) payload
                    : objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish event to " + topic, e);
        }
    }

    public void publish(String topic, String key, Object payload) {
        try {
            String message = payload instanceof String
                    ? (String) payload
                    : objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish event to " + topic, e);
        }
    }
}
