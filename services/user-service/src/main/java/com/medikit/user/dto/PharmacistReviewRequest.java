package com.medikit.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record PharmacistReviewRequest(
        @NotNull UUID verificationId,
        boolean approved,
        @Size(max = 500) String rejectionReason
) {
}
