package com.medikit.distributor.repository;

import com.medikit.distributor.entity.DistributorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DistributorProfileRepository extends JpaRepository<DistributorProfile, UUID> {

    Optional<DistributorProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<DistributorProfile> findByActiveTrueOrderByCreatedAtAsc();
}
