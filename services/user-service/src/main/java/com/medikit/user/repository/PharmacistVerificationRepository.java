package com.medikit.user.repository;

import com.medikit.user.entity.PharmacistVerification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PharmacistVerificationRepository extends JpaRepository<PharmacistVerification, UUID> {

    Optional<PharmacistVerification> findByUserId(UUID userId);

    Optional<PharmacistVerification> findByLicenseNumber(String licenseNumber);

    Page<PharmacistVerification> findByStatus(PharmacistVerification.VerificationStatus status, Pageable pageable);
}
