package com.medikit.discount.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CampaignResponse(
        UUID id,
        String name,
        String description,
        String discountType,
        BigDecimal discountAmount,
        BigDecimal percentage,
        int validForDays,
        int totalCodes,
        int issuedCodes,
        boolean active,
        boolean firstOrderOnly,
        Instant createdAt
) {
}
