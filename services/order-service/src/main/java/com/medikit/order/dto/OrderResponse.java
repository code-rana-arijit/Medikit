package com.medikit.order.dto;

import com.medikit.order.entity.Order;
import com.medikit.order.entity.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String orderNumber,
        UUID userId,
        UUID pharmacyId,
        String status,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal discount,
        BigDecimal total,
        String paymentMethod,
        String paymentStatus,
        String deliveryAddress,
        String deliverySlotId,
        String cancellationReason,
        Instant createdAt,
        Instant confirmedAt,
        List<OrderItemResponse> items
) {

    public record OrderItemResponse(
            UUID productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal mrp,
            BigDecimal lineTotal,
            boolean prescriptionRequired
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getMrp(),
                    item.getLineTotal(),
                    item.isPrescriptionRequired());
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUserId(),
                order.getPharmacyId(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getDiscount(),
                order.getTotal(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getDeliveryAddress(),
                order.getDeliverySlotId(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                order.getConfirmedAt(),
                order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}
