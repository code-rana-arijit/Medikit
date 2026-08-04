package com.medikit.order.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.NotFoundException;
import com.medikit.order.dto.CreateOrderRequest;
import com.medikit.order.dto.OrderResponse;
import com.medikit.order.entity.Order;
import com.medikit.order.entity.OrderItem;
import com.medikit.order.entity.OrderStatus;
import com.medikit.order.repository.OrderRepository;
import com.medikit.order.saga.OrderSagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("29.00");

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;

    public OrderService(OrderRepository orderRepository, OrderSagaOrchestrator sagaOrchestrator) {
        this.orderRepository = orderRepository;
        this.sagaOrchestrator = sagaOrchestrator;
    }

    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest request) {
        BigDecimal subtotal = request.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = request.items().stream()
                .map(item -> item.mrp().subtract(item.unitPrice()).multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(BigDecimal.ZERO);

        BigDecimal total = subtotal.add(DELIVERY_FEE);

        Order order = Order.builder()
                .userId(userId)
                .pharmacyId(request.pharmacyId())
                .orderNumber(generateOrderNumber())
                .status(OrderStatus.CREATED)
                .subtotal(subtotal)
                .deliveryFee(DELIVERY_FEE)
                .discount(discount)
                .total(total)
                .paymentMethod(request.paymentMethod())
                .paymentStatus("PENDING")
                .deliveryAddress(request.address() != null ? request.address().address() : null)
                .latitude(request.address() != null ? request.address().latitude() : null)
                .longitude(request.address() != null ? request.address().longitude() : null)
                .deliverySlotId(request.deliverySlotId())
                .build();

        request.items().forEach(item -> order.addItem(OrderItem.builder()
                .productId(item.productId())
                .productName(item.productName() != null ? item.productName() : "Medicine")
                .quantity(item.quantity())
                .unitPrice(item.unitPrice())
                .mrp(item.mrp())
                .lineTotal(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .prescriptionRequired(item.prescriptionRequired())
                .build()));

        Order saved = orderRepository.save(order);
        log.info("Order {} created for user {}", saved.getId(), userId);

        boolean reserved = sagaOrchestrator.reserveInventory(saved);
        if (reserved) {
            sagaOrchestrator.initiatePayment(saved);
        }

        return OrderResponse.from(orderRepository.findById(saved.getId())
                .orElse(saved));
    }

    public OrderResponse getOrder(UUID orderId) {
        return OrderResponse.from(findOrder(orderId));
    }

    public Page<OrderResponse> getUserOrders(UUID userId, int page, int size) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(OrderResponse::from);
    }

    public Page<OrderResponse> getPharmacyOrders(UUID pharmacyId, int page, int size) {
        return orderRepository.findByPharmacyIdOrderByCreatedAtDesc(pharmacyId, PageRequest.of(page, size))
                .map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID userId, UUID orderId, String reason) {
        Order order = findOrder(orderId);
        if (!order.getUserId().equals(userId)) {
            throw new NotFoundException("Order not found");
        }
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.FAILED) {
            throw new BadRequestException("Order cannot be cancelled in current state");
        }
        sagaOrchestrator.compensate(order, reason != null ? reason : "Cancelled by user");
        return OrderResponse.from(findOrder(orderId));
    }

    @Transactional
    public OrderResponse updateStatus(UUID orderId, String status) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        order.setStatus(OrderStatus.valueOf(status));
        if (OrderStatus.DELIVERED == order.getStatus()) {
            order.setSagaComplete(true);
        }
        return OrderResponse.from(orderRepository.save(order));
    }

    public List<OrderResponse> findStaleOrders(int maxAgeMinutes) {
        Instant cutoff = Instant.now().minusSeconds(maxAgeMinutes * 60L);
        return orderRepository.findStaleOrders(
                        List.of(OrderStatus.CREATED, OrderStatus.PENDING_PAYMENT), cutoff)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    private Order findOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private String generateOrderNumber() {
        return "MDK" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now())
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
