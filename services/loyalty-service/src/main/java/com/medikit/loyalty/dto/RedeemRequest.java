package com.medikit.loyalty.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RedeemRequest(
        @NotNull(message = "Points to redeem is required")
        @Min(value = 100, message = "Minimum redemption is 100 points")
        long points) {
}
