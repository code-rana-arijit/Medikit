package com.medikit.discount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ValidateDiscountRequest(
        @NotBlank(message = "Code is required")
        String code,

        @NotNull(message = "User id is required")
        UUID userId
) {
}
