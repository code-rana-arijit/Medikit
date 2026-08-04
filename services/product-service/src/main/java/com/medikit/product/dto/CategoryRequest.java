package com.medikit.product.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        String name,

        String description,
        int sortOrder
) {
}
