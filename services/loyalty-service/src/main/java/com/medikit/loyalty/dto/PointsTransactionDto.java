package com.medikit.loyalty.dto;

import com.medikit.loyalty.model.TransactionType;

import java.time.Instant;
import java.util.UUID;

public record PointsTransactionDto(
        UUID orderId,
        TransactionType type,
        long points,
        String description,
        Instant createdAt) {
}
