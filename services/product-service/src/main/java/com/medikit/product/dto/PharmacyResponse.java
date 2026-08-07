package com.medikit.product.dto;

import com.medikit.product.entity.Pharmacy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PharmacyResponse(
        UUID id,
        String name,
        String licenseNumber,
        String licenseState,
        LocalDate licenseExpiry,
        String licenseStatus,
        String address,
        String phone,
        UUID ownerUserId,
        boolean active,
        Instant createdAt
) {
    public static PharmacyResponse from(Pharmacy p) {
        return new PharmacyResponse(
                p.getId(),
                p.getName(),
                p.getLicenseNumber(),
                p.getLicenseState(),
                p.getLicenseExpiry(),
                p.getLicenseStatus().name(),
                p.getAddress(),
                p.getPhone(),
                p.getOwnerUserId(),
                p.isActive(),
                p.getCreatedAt());
    }
}
