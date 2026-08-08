package com.medikit.discount.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record IssueDiscountRequest(
        @NotNull(message = "User id is required")
        UUID userId,

        @NotNull(message = "Discount amount is required")
        @DecimalMin(value = "0.01", message = "Discount amount must be positive")
        BigDecimal discountAmount,

        Integer validForDays
) {
}
