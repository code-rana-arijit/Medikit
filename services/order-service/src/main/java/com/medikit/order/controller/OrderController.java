package com.medikit.order.controller;

import com.medikit.common.security.UserContext;
import com.medikit.common.web.ForbiddenException;
import com.medikit.common.web.PageResult;
import com.medikit.order.dto.CancelOrderRequest;
import com.medikit.order.dto.CreateOrderRequest;
import com.medikit.order.dto.OrderResponse;
import com.medikit.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        UUID userId = UUID.fromString(UserContext.currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(userId, request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        String callerId = UserContext.currentUserId();
        String callerRole = UserContext.currentUserRole();
        OrderResponse order = orderService.getOrder(orderId);
        if (callerRole != null && "CUSTOMER".equalsIgnoreCase(callerRole)
                && callerId != null && !callerId.equals(order.userId().toString())) {
            throw new ForbiddenException("Order not found");
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping
    public ResponseEntity<PageResult<OrderResponse>> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = UUID.fromString(UserContext.currentUserId());
        Page<OrderResponse> result = orderService.getUserOrders(userId, page, Math.min(size, 50));
        return ResponseEntity.ok(PageResult.from(result));
    }

    @GetMapping("/pharmacy/{pharmacyId}")
    public ResponseEntity<PageResult<OrderResponse>> pharmacyOrders(
            @PathVariable UUID pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String callerRole = UserContext.currentUserRole();
        if (callerRole == null || "CUSTOMER".equalsIgnoreCase(callerRole)) {
            throw new ForbiddenException("Pharmacy orders are restricted");
        }
        Page<OrderResponse> result = orderService.getPharmacyOrders(pharmacyId, page, Math.min(size, 50));
        return ResponseEntity.ok(PageResult.from(result));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID orderId,
                                                     @RequestBody(required = false) CancelOrderRequest request) {
        UUID userId = UUID.fromString(UserContext.currentUserId());
        return ResponseEntity.ok(orderService.cancelOrder(userId, orderId,
                request != null ? request.reason() : null));
    }

    @PostMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable UUID orderId,
                                                      @RequestParam String status) {
        if (!"ADMIN".equalsIgnoreCase(UserContext.currentUserRole())) {
            throw new ForbiddenException("Only admins can update order status");
        }
        return ResponseEntity.ok(orderService.updateStatus(orderId, status));
    }

    @GetMapping("/internal/stale")
    public ResponseEntity<List<OrderResponse>> staleOrders(@RequestParam(defaultValue = "10") int maxAgeMinutes) {
        return ResponseEntity.ok(orderService.findStaleOrders(maxAgeMinutes));
    }
}
