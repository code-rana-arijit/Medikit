package com.medikit.order.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CancelOrderRequest(
        @NotBlank(message = "Cancellation reason is required")
        String reason
) {
}
