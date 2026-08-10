package com.medikit.distributor.repository;

import com.medikit.distributor.entity.DistributorOrder;
import com.medikit.distributor.entity.DistributorOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DistributorOrderRepository extends JpaRepository<DistributorOrder, UUID> {

    Page<DistributorOrder> findByBuyerUserId(UUID buyerUserId, Pageable pageable);

    Page<DistributorOrder> findByDistributorId(UUID distributorId, Pageable pageable);

    Page<DistributorOrder> findByBuyerUserIdAndStatus(UUID buyerUserId, DistributorOrderStatus status, Pageable pageable);

    Page<DistributorOrder> findByDistributorIdAndStatus(UUID distributorId, DistributorOrderStatus status, Pageable pageable);
}
