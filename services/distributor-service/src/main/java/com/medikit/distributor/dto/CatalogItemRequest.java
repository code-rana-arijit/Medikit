package com.medikit.distributor.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogItemRequest(
        @NotNull(message = "Product id is required")
        UUID productId,

        @NotBlank(message = "Product name is required")
        @Size(max = 150, message = "Product name must be at most 150 characters")
        String productName,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.01", message = "Unit price must be positive")
        BigDecimal unitPrice,

        @Min(value = 1, message = "Pack size must be at least 1")
        int packSize,

        @Min(value = 0, message = "Stock quantity cannot be negative")
        int stockQty
) {
}
