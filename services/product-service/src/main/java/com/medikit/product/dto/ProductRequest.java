package com.medikit.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank(message = "Product name is required")
        String name,

        String description,
        String saltComposition,
        String manufacturer,

        @NotNull(message = "MRP is required")
        @DecimalMin(value = "0.0", message = "MRP must be positive")
        BigDecimal mrp,

        @NotNull(message = "Selling price is required")
        @DecimalMin(value = "0.0", message = "Selling price must be positive")
        BigDecimal sellingPrice,

        boolean prescriptionRequired,

        @NotNull(message = "Product type is required")
        String productType,

        String packaging,
        String packSize,
        UUID categoryId,

        @NotNull(message = "Pharmacy is required")
        UUID pharmacyId,

        String imageUrl
) {
}
