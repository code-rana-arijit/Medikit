package com.medikit.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "Pharmacy is required")
        UUID pharmacyId,

        @NotEmpty(message = "Order must contain at least one item")
        @Size(max = 50, message = "Order cannot contain more than 50 items")
        List<@Valid OrderItemRequest> items,

        @NotNull(message = "Delivery address is required")
        @Valid
        AddressInfo address,

        String paymentMethod,
        String deliverySlotId,

        String discountCode
) {

    public record OrderItemRequest(
            @NotNull(message = "Product id is required")
            UUID productId,

            String productName,

            @NotNull(message = "Quantity is required")
            int quantity,

            @NotNull(message = "Unit price is required")
            BigDecimal unitPrice,

            @NotNull(message = "MRP is required")
            BigDecimal mrp,

            boolean prescriptionRequired
    ) {
    }

    public record AddressInfo(
            @NotNull(message = "Address is required")
            String address,

            Double latitude,
            Double longitude
    ) {
    }
}
