package com.medikit.product.repository;

import com.medikit.product.entity.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PharmacyRepository extends JpaRepository<Pharmacy, UUID> {

    Optional<Pharmacy> findByLicenseNumber(String licenseNumber);
}
