package com.medikit.distributor.dto;

import com.medikit.distributor.entity.DistributorOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DistributorOrderResponse(
        UUID id,
        String orderNumber,
        UUID buyerUserId,
        UUID distributorId,
        DistributorOrderStatus status,
        BigDecimal totalAmount,
        List<DistributorOrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
