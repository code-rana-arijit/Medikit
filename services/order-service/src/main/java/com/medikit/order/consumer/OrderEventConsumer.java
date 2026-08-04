package com.medikit.order.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.event.Topics;
import com.medikit.order.entity.Order;
import com.medikit.order.entity.OrderStatus;
import com.medikit.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(OrderRepository orderRepository, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.DELIVERY_UPDATED, groupId = "order-service")
    public void onDeliveryUpdated(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String orderId = node.path("orderId").asText();
            String status = node.path("status").asText();

            orderRepository.findById(UUID.fromString(orderId)).ifPresent(order -> {
                if ("DELIVERED".equalsIgnoreCase(status)) {
                    order.setStatus(OrderStatus.DELIVERED);
                    orderRepository.save(order);
                } else if ("IN_TRANSIT".equalsIgnoreCase(status)) {
                    order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
                    orderRepository.save(order);
                }
            });
        } catch (Exception e) {
            log.error("Failed to process delivery update event", e);
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED, groupId = "order-service")
    public void onPaymentFailed(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            String orderId = node.path("orderId").asText();
            orderRepository.findById(UUID.fromString(orderId)).ifPresent(order -> {
                order.setPaymentStatus("FAILED");
                order.setStatus(OrderStatus.FAILED);
                orderRepository.save(order);
            });
        } catch (Exception e) {
            log.error("Failed to process payment failure event", e);
        }
    }
}
