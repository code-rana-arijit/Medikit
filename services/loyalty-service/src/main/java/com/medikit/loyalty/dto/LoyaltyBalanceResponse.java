package com.medikit.loyalty.dto;

import com.medikit.loyalty.model.LoyaltyTier;

import java.math.BigDecimal;

public record LoyaltyBalanceResponse(
        long balancePoints,
        long lifetimeEarned,
        BigDecimal totalSpend,
        LoyaltyTier tier,
        BigDecimal nextTierThreshold,
        double earnMultiplier) {
}
