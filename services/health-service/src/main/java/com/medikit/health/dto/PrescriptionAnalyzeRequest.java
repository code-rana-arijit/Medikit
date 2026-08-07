package com.medikit.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PrescriptionAnalyzeRequest(
        @NotBlank(message = "Prescription text is required")
        @Size(max = 10000, message = "Prescription text is too long")
        String text,
        @Size(max = 50, message = "Up to 50 order items can be cross-checked")
        List<@NotBlank String> orderItems) {
}
