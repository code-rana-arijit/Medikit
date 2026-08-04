package com.medikit.prescription.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PrescriptionUploadRequest(
        @NotBlank(message = "Patient name is required")
        String patientName,

        @NotNull(message = "Patient age is required")
        @Min(value = 0, message = "Patient age must be positive")
        @Max(value = 150, message = "Patient age must be at most 150")
        int patientAge,

        String doctorName,
        String diagnosis,
        String imageUrl
) {
}
