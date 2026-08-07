package com.medikit.user.dto;

import com.medikit.user.entity.PharmacistVerification;

import java.time.Instant;
import java.util.UUID;

public record PharmacistVerificationResponse(
        UUID id,
        UUID userId,
        String licenseNumber,
        String licenseState,
        String fullName,
        String licenseDocumentUrl,
        String status,
        String rejectionReason,
        Instant reviewedAt,
        Instant createdAt
) {
    public static PharmacistVerificationResponse from(PharmacistVerification v) {
        return new PharmacistVerificationResponse(
                v.getId(),
                v.getUserId(),
                v.getLicenseNumber(),
                v.getLicenseState(),
                v.getFullName(),
                v.getLicenseDocumentUrl(),
                v.getStatus().name(),
                v.getRejectionReason(),
                v.getReviewedAt(),
                v.getCreatedAt());
    }
}
