package com.medikit.delivery.dto;

import jakarta.validation.constraints.NotBlank;

public record DeliveryStatusRequest(
        @NotBlank String status,
        Coordinates coordinates
) {

    public record Coordinates(Double latitude, Double longitude) {
    }
}
