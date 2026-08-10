package com.medikit.distributor.dto;

import com.medikit.distributor.entity.FulfillmentStatus;

import java.time.Instant;
import java.util.UUID;

public record FulfillmentResponse(
        UUID id,
        UUID orderId,
        UUID customerUserId,
        UUID distributorId,
        FulfillmentStatus status,
        Instant createdAt,
        Instant deliveredAt
) {
}
