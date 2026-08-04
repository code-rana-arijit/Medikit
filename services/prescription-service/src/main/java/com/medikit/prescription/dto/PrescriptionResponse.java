package com.medikit.prescription.dto;

import com.medikit.prescription.entity.Prescription;

import java.time.Instant;
import java.util.UUID;

public record PrescriptionResponse(
        UUID id,
        UUID userId,
        String patientName,
        int patientAge,
        String doctorName,
        String diagnosis,
        String status,
        String imageUrl,
        String rejectionReason,
        Instant approvedAt,
        Instant expiresAt,
        Instant createdAt
) {
    public static PrescriptionResponse from(Prescription p) {
        return new PrescriptionResponse(
                p.getId(),
                p.getUserId(),
                p.getPatientName(),
                p.getPatientAge(),
                p.getDoctorName(),
                p.getDiagnosis(),
                p.getStatus().name(),
                p.getImageUrl(),
                p.getRejectionReason(),
                p.getApprovedAt(),
                p.getExpiresAt(),
                p.getCreatedAt()
        );
    }
}
