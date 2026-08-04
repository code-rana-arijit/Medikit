package com.medikit.inventory.dto;

import java.util.List;
import java.util.UUID;

public record StockReservationResponse(
        UUID orderId,
        String status,
        List<ReservedItem> items
) {
    public record ReservedItem(
            UUID productId,
            int quantity,
            int availableQuantity
    ) {
    }
}
