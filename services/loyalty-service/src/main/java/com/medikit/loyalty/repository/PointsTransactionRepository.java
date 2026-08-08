package com.medikit.loyalty.repository;

import com.medikit.loyalty.entity.PointsTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {

    Page<PointsTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    boolean existsByOrderId(UUID orderId);
}
