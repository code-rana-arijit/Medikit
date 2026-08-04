package com.medikit.delivery.dto;

import java.time.Instant;
import java.util.UUID;

public record DeliveryResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        UUID pharmacyId,
        UUID slotId,
        String status,
        UUID partnerId,
        Double customerLatitude,
        Double customerLongitude,
        Double partnerLatitude,
        Double partnerLongitude,
        int estimatedMinutes,
        Instant deliveredAt,
        Instant createdAt,
        Instant updatedAt
) {
}
