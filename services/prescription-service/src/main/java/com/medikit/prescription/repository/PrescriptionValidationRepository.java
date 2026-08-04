package com.medikit.prescription.repository;

import com.medikit.prescription.entity.PrescriptionValidation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PrescriptionValidationRepository extends JpaRepository<PrescriptionValidation, UUID> {

    List<PrescriptionValidation> findByPrescriptionIdOrderByCreatedAtDesc(UUID prescriptionId);
}
