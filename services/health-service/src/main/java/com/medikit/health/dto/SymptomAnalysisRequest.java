package com.medikit.health.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SymptomAnalysisRequest(
        @NotEmpty(message = "At least one symptom is required")
        @Size(max = 20, message = "Up to 20 symptoms can be analyzed at once")
        List<@NotEmpty String> symptoms) {
}
