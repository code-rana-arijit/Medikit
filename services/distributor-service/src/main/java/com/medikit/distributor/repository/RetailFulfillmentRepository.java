package com.medikit.distributor.repository;

import com.medikit.distributor.entity.RetailFulfillment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RetailFulfillmentRepository extends JpaRepository<RetailFulfillment, UUID> {

    Optional<RetailFulfillment> findByOrderId(UUID orderId);

    List<RetailFulfillment> findByDistributorIdOrderByCreatedAtDesc(UUID distributorId);

    boolean existsByOrderId(UUID orderId);
}
