package com.medikit.distributor.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record DistributorOrderItemResponse(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
