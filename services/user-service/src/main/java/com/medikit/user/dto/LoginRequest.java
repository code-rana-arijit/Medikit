package com.medikit.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "Phone or email is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {
}
