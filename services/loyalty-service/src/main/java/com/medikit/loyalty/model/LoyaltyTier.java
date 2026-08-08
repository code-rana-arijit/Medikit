package com.medikit.loyalty.model;

import java.math.BigDecimal;

public enum LoyaltyTier {

    BRONZE(new BigDecimal("0"), 1.0),
    SILVER(new BigDecimal("5000"), 1.1),
    GOLD(new BigDecimal("25000"), 1.25),
    PLATINUM(new BigDecimal("100000"), 1.5);

    private final BigDecimal spendThreshold;
    private final double earnMultiplier;

    LoyaltyTier(BigDecimal spendThreshold, double earnMultiplier) {
        this.spendThreshold = spendThreshold;
        this.earnMultiplier = earnMultiplier;
    }

    public BigDecimal spendThreshold() {
        return spendThreshold;
    }

    public double earnMultiplier() {
        return earnMultiplier;
    }

    public static LoyaltyTier fromTotalSpend(BigDecimal totalSpend) {
        LoyaltyTier highest = BRONZE;
        for (LoyaltyTier tier : values()) {
            if (totalSpend.compareTo(tier.spendThreshold()) >= 0 && tier.ordinal() > highest.ordinal()) {
                highest = tier;
            }
        }
        return highest;
    }
}
