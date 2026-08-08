package com.medikit.loyalty.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "medikit.loyalty")
public record LoyaltyProperties(
        BigDecimal spendPerPoint,
        BigDecimal pointsPerRedemptionUnit,
        BigDecimal redemptionUnitValue) {

    public LoyaltyProperties {
        if (spendPerPoint == null || spendPerPoint.signum() <= 0) {
            spendPerPoint = new BigDecimal("100");
        }
        if (pointsPerRedemptionUnit == null || pointsPerRedemptionUnit.signum() <= 0) {
            pointsPerRedemptionUnit = new BigDecimal("100");
        }
        if (redemptionUnitValue == null || redemptionUnitValue.signum() < 0) {
            redemptionUnitValue = new BigDecimal("10");
        }
    }

    public static LoyaltyProperties defaults() {
        return new LoyaltyProperties(new BigDecimal("100"), new BigDecimal("100"), new BigDecimal("10"));
    }
}
