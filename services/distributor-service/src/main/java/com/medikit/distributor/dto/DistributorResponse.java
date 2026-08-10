package com.medikit.distributor.dto;

import java.time.Instant;
import java.util.UUID;

public record DistributorResponse(
        UUID id,
        UUID userId,
        String shopName,
        String licenseNumber,
        String address,
        String city,
        boolean active,
        Instant createdAt
) {
}
