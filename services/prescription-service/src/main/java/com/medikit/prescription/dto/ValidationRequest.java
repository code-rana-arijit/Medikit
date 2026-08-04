package com.medikit.prescription.dto;

import com.medikit.prescription.entity.ValidationDecision;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ValidationRequest(
        @NotNull(message = "Validator id is required")
        UUID validatorId,

        @NotNull(message = "Decision is required")
        ValidationDecision decision,

        String comments,

        String rejectionReason
) {
}
