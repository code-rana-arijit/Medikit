package com.medikit.discount.dto;

import com.medikit.discount.model.DiscountType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateCampaignRequest(
        @NotBlank(message = "Campaign name is required")
        @Size(max = 120, message = "Campaign name must be at most 120 characters")
        String name,

        @Size(max = 300, message = "Description must be at most 300 characters")
        String description,

        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @DecimalMin(value = "0.01", message = "Discount amount must be positive")
        BigDecimal discountAmount,

        @DecimalMin(value = "0.1", message = "Percentage must be positive")
        @DecimalMax(value = "100", message = "Percentage must be at most 100")
        BigDecimal percentage,

        @Min(value = 1, message = "Valid for days must be at least 1")
        int validForDays,

        @Min(value = 1, message = "Total codes must be at least 1")
        @Max(value = 10000, message = "Total codes must be at most 10000")
        int totalCodes,

        boolean firstOrderOnly
) {
}
