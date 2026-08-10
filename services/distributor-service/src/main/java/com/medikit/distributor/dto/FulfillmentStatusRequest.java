package com.medikit.distributor.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record FulfillmentStatusRequest(
        @NotNull(message = "Status is required")
        String status
) {
}
