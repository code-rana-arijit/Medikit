package com.medikit.discount.dto;

import com.medikit.discount.model.DiscountType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record IssueDiscountRequest(
        @NotNull(message = "User id is required")
        UUID userId,

        DiscountType discountType,

        @DecimalMin(value = "0.01", message = "Discount amount must be positive")
        @Digits(integer = 10, fraction = 2, message = "Discount amount must have at most 2 decimals")
        BigDecimal discountAmount,

        @DecimalMin(value = "0.1", message = "Percentage must be positive")
        @DecimalMax(value = "100", message = "Percentage must be at most 100")
        BigDecimal percentage,

        UUID campaignId,

        String title,

        Boolean firstOrderOnly,

        Integer validForDays
) {

    public DiscountType effectiveType() {
        return discountType != null ? discountType : DiscountType.FIXED;
    }
}
