package com.medikit.user.dto;

import java.time.Instant;
import java.util.UUID;

public record AddressResponse(
        UUID id,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String pincode,
        Double latitude,
        Double longitude,
        String type,
        boolean isDefault,
        Instant createdAt
) {
}
