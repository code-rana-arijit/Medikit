package com.medikit.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StockUpdateRequest(
        @NotNull(message = "Product id is required")
        UUID productId,

        @NotNull(message = "Pharmacy id is required")
        UUID pharmacyId,

        @Min(value = 0, message = "Quantity must not be negative")
        int quantityAvailable,

        Integer minStockLevel,
        Integer maxStockLevel,
        Boolean active
) {
    public int minStockLevelOrDefault() {
        return minStockLevel != null ? minStockLevel : 0;
    }

    public int maxStockLevelOrDefault() {
        return maxStockLevel != null ? maxStockLevel : 1000;
    }

    public boolean activeOrDefault() {
        return active != null ? active : true;
    }
}
