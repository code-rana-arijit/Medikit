package com.medikit.inventory.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record StockBulkRequest(
        @NotNull(message = "Pharmacy id is required")
        UUID pharmacyId,

        @NotEmpty(message = "At least one product id is required")
        List<UUID> productIds
) {
}
