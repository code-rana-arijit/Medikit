package com.medikit.distributor.controller;

import com.medikit.common.security.UserContext;
import com.medikit.common.web.PageResult;
import com.medikit.distributor.dto.DistributorOrderRequest;
import com.medikit.distributor.dto.DistributorOrderResponse;
import com.medikit.distributor.entity.DistributorOrderStatus;
import com.medikit.distributor.service.DistributorOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/distributor-orders")
public class DistributorOrderController {

    private final DistributorOrderService orderService;

    public DistributorOrderController(DistributorOrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<DistributorOrderResponse> place(@Valid @RequestBody DistributorOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.placeOrder(currentUserId(), request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<DistributorOrderResponse> get(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(currentUserId(), orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<DistributorOrderResponse> updateStatus(@PathVariable UUID orderId,
                                                                 @RequestParam String status) {
        DistributorOrderStatus target;
        try {
            target = DistributorOrderStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.medikit.common.web.BadRequestException("Invalid order status");
        }
        return ResponseEntity.ok(orderService.updateStatus(currentUserId(), orderId, target));
    }

    @GetMapping
    public ResponseEntity<PageResult<DistributorOrderResponse>> myOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.myOrders(currentUserId(), page, size, status));
    }

    @GetMapping("/distributor")
    public ResponseEntity<PageResult<DistributorOrderResponse>> distributorOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.distributorOrders(currentUserId(), page, size, status));
    }

    private UUID currentUserId() {
        return UUID.fromString(UserContext.currentUserId());
    }
}
