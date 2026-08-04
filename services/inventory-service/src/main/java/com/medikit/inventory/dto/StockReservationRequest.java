package com.medikit.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record StockReservationRequest(
        @NotNull(message = "Order id is required")
        UUID orderId,

        @NotNull(message = "Pharmacy id is required")
        UUID pharmacyId,

        @NotEmpty(message = "At least one item is required")
        List<Item> items
) {
    public record Item(
            @NotNull(message = "Product id is required")
            UUID productId,

            @Min(value = 1, message = "Quantity must be positive")
            int quantity
    ) {
    }
}
