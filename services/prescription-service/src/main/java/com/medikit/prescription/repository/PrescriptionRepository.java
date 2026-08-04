package com.medikit.prescription.repository;

import com.medikit.prescription.entity.Prescription;
import com.medikit.prescription.entity.PrescriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    Page<Prescription> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<Prescription> findByStatusAndCreatedAtBefore(PrescriptionStatus status, Instant cutoff);
}
