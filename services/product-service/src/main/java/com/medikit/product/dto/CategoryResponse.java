package com.medikit.product.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        String description,
        int sortOrder,
        Instant createdAt
) {
}
