package com.medikit.user.repository;

import com.medikit.user.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId);

    long countByUserId(UUID userId);
}
