package com.medikit.cart.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record AddItemRequest(
        @NotNull(message = "Product id is required")
        UUID productId,

        @NotNull(message = "Pharmacy id is required")
        UUID pharmacyId,

        @NotBlank(message = "Product name is required")
        String productName,

        @NotNull(message = "Unit price is required")
        BigDecimal unitPrice,

        @NotNull(message = "MRP is required")
        BigDecimal mrp,

        @NotNull(message = "Quantity is required")
        Integer quantity,

        String imageUrl,

        boolean prescriptionRequired
) {
}
