package com.medikit.prescription.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "prescription_validations")
public class PrescriptionValidation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "prescription_id", nullable = false)
    private UUID prescriptionId;

    @Column(name = "validator_id", nullable = false)
    private UUID validatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ValidationDecision decision;

    @Column(length = 500)
    private String comments;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;
}
