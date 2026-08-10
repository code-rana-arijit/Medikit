package com.medikit.distributor.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CatalogItemResponse(
        UUID id,
        UUID distributorId,
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        int packSize,
        int stockQty,
        Instant createdAt
) {
}
