package com.medikit.order.saga;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.event.EventPublisher;
import com.medikit.common.event.Topics;
import com.medikit.order.client.InventoryClient;
import com.medikit.order.client.PaymentClient;
import com.medikit.order.entity.Order;
import com.medikit.order.entity.OrderItem;
import com.medikit.order.entity.OrderStatus;
import com.medikit.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the create-order saga:
 * 1. Reserve inventory
 * 2. Initiate payment (COD skips payment)
 * 3. Confirm order
 * On any failure, compensating transactions release inventory and roll back.
 */
@Component
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public OrderSagaOrchestrator(OrderRepository orderRepository,
                                 InventoryClient inventoryClient,
                                 PaymentClient paymentClient,
                                 EventPublisher eventPublisher,
                                 ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Retry(name = "inventoryClient")
    @CircuitBreaker(name = "inventoryClient")
    @Transactional
    public boolean reserveInventory(Order order) {
        List<Map<String, Object>> items = order.getItems().stream()
                .map(item -> Map.<String, Object>of(
                        "productId", item.getProductId().toString(),
                        "quantity", item.getQuantity()))
                .toList();

        Map<String, Object> request = Map.of(
                "orderId", order.getId().toString(),
                "pharmacyId", order.getPharmacyId().toString(),
                "items", items);

        try {
            Map<String, Object> response = inventoryClient.reserveStock(request);
            Boolean success = (Boolean) response.getOrDefault("success", false);
            if (Boolean.TRUE.equals(success)) {
                order.setStatus(OrderStatus.PENDING_PAYMENT);
                orderRepository.save(order);
                return true;
            } else {
                failOrder(order, "Insufficient stock");
                return false;
            }
        } catch (Exception e) {
            log.error("Inventory reservation failed for order {}", order.getId(), e);
            failOrder(order, "Inventory reservation failed");
            return false;
        }
    }

    @Transactional
    public boolean initiatePayment(Order order) {
        if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
            order.setPaymentStatus("COD_CONFIRMED");
            confirmOrder(order);
            return true;
        }

        Map<String, Object> request = Map.of(
                "orderId", order.getId().toString(),
                "amount", order.getTotal(),
                "currency", "INR",
                "method", order.getPaymentMethod() != null ? order.getPaymentMethod() : "CARD");

        try {
            Map<String, Object> response = paymentClient.initiatePayment(request);
            String status = String.valueOf(response.getOrDefault("status", "FAILED"));
            if ("INITIATED".equals(status) || "COMPLETED".equals(status)) {
                order.setPaymentStatus(status);
                confirmOrder(order);
                return true;
            } else {
                compensate(order, "Payment failed");
                return false;
            }
        } catch (Exception e) {
            log.error("Payment initiation failed for order {}", order.getId(), e);
            compensate(order, "Payment initiation failed");
            return false;
        }
    }

    @Transactional
    public void confirmOrder(Order order) {
        order.setStatus(OrderStatus.CONFIRMED);
        order.setSagaComplete(true);
        order.setConfirmedAt(java.time.Instant.now());
        orderRepository.save(order);
        eventPublisher.publish(Topics.ORDER_CONFIRMED, order.getId().toString(), toPayload(order));
    }

    @Transactional
    public void compensate(Order order, String reason) {
        releaseInventory(order);
        order.setStatus(OrderStatus.FAILED);
        order.setCancellationReason(reason);
        order.setSagaComplete(true);
        orderRepository.save(order);
        eventPublisher.publish(Topics.ORDER_FAILED, order.getId().toString(),
                Map.of("orderId", order.getId().toString(), "reason", reason));
    }

    @Transactional
    public void failOrder(Order order, String reason) {
        order.setStatus(OrderStatus.FAILED);
        order.setCancellationReason(reason);
        order.setSagaComplete(true);
        orderRepository.save(order);
        eventPublisher.publish(Topics.ORDER_FAILED, order.getId().toString(),
                Map.of("orderId", order.getId().toString(), "reason", reason));
    }

    private void releaseInventory(Order order) {
        try {
            inventoryClient.releaseReservation(Map.of("orderId", order.getId().toString()));
        } catch (Exception e) {
            log.error("Inventory release failed for order {}", order.getId(), e);
        }
    }

    private Map<String, Object> toPayload(Order order) {
        return Map.of(
                "orderId", order.getId().toString(),
                "userId", order.getUserId().toString(),
                "pharmacyId", order.getPharmacyId().toString(),
                "total", order.getTotal(),
                "items", order.getItems().stream()
                        .map(item -> Map.of(
                                "productId", item.getProductId().toString(),
                                "quantity", item.getQuantity(),
                                "unitPrice", item.getUnitPrice()))
                        .toList());
    }
}
