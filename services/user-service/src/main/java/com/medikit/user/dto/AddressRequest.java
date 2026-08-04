package com.medikit.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddressRequest(
        @NotBlank(message = "Address line 1 is required")
        String addressLine1,

        String addressLine2,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "Pincode is required")
        String pincode,

        @NotNull(message = "Latitude is required")
        Double latitude,

        @NotNull(message = "Longitude is required")
        Double longitude,

        String type
) {
}
