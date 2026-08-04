package com.medikit.inventory.dto;

import com.medikit.inventory.entity.InventoryItem;

import java.time.Instant;
import java.util.UUID;

public record StockLevelResponse(
        UUID productId,
        UUID pharmacyId,
        int quantityAvailable,
        int reservedQuantity,
        int availableQuantity,
        int minStockLevel,
        int maxStockLevel,
        boolean active,
        Instant updatedAt
) {
    public static StockLevelResponse from(InventoryItem item) {
        return new StockLevelResponse(
                item.getProductId(),
                item.getPharmacyId(),
                item.getQuantityAvailable(),
                item.getReservedQuantity(),
                item.availableQuantity(),
                item.getMinStockLevel(),
                item.getMaxStockLevel(),
                item.isActive(),
                item.getUpdatedAt()
        );
    }
}
