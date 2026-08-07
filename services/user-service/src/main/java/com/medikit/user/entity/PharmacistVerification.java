package com.medikit.user.entity;

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
@Table(name = "pharmacist_verifications", indexes = {
        @Index(name = "idx_pharm_verif_user", columnList = "userId"),
        @Index(name = "idx_pharm_verif_status", columnList = "status"),
        @Index(name = "idx_pharm_verif_license", columnList = "licenseNumber", unique = true)
})
public class PharmacistVerification {

    public enum VerificationStatus {
        PENDING, VERIFIED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 100)
    private String licenseNumber;

    @Column(nullable = false, length = 150)
    private String licenseState;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 500)
    private String licenseDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(length = 500)
    private String rejectionReason;

    @Column(length = 150)
    private String reviewedBy;

    private Instant reviewedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
