package com.medikit.distributor.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FulfillmentClaimRequest(
        @NotNull(message = "Order id is required")
        UUID orderId
) {
}
