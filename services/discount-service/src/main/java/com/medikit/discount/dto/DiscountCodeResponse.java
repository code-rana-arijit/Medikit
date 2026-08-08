package com.medikit.discount.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DiscountCodeResponse(
        String code,
        UUID userId,
        BigDecimal discountAmount,
        String currency,
        String status,
        Instant expiresAt,
        Instant createdAt,
        Instant redeemedAt,
        UUID redeemedOrderId
) {
}
