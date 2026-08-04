package com.medikit.prescription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prescriptions", indexes = {
        @Index(name = "idx_prescriptions_user", columnList = "user_id"),
        @Index(name = "idx_prescriptions_status", columnList = "status")
})
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "patient_name", nullable = false, length = 255)
    private String patientName;

    @Column(name = "patient_age", nullable = false)
    private int patientAge;

    @Column(name = "doctor_name", length = 255)
    private String doctorName;

    @Column(length = 500)
    private String diagnosis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PrescriptionStatus status;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
