package com.medikit.health.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record InteractionCheckRequest(
        @NotEmpty(message = "At least one drug is required")
        @Size(max = 50, message = "Up to 50 drugs can be checked at once")
        List<@NotEmpty String> drugs) {
}
