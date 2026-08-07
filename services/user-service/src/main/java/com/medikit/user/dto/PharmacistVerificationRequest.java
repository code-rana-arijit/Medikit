package com.medikit.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PharmacistVerificationRequest(
        @NotBlank @Size(max = 100) String licenseNumber,
        @NotBlank @Size(max = 150) String licenseState,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 500) String licenseDocumentUrl
) {
}
