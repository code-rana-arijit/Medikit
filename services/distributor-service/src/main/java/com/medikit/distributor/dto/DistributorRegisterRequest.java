package com.medikit.distributor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DistributorRegisterRequest(
        @NotBlank(message = "Shop name is required")
        @Size(min = 2, max = 150, message = "Shop name must be between 2 and 150 characters")
        String shopName,

        @NotBlank(message = "License number is required")
        @Size(max = 60, message = "License number must be at most 60 characters")
        String licenseNumber,

        @NotBlank(message = "Address is required")
        @Size(max = 200, message = "Address must be at most 200 characters")
        String address,

        @NotBlank(message = "City is required")
        @Size(max = 80, message = "City must be at most 80 characters")
        String city
) {
}
