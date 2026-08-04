package com.medikit.order.repository;

import com.medikit.order.entity.Order;
import com.medikit.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Order> findByPharmacyIdOrderByCreatedAtDesc(UUID pharmacyId, Pageable pageable);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT o FROM Order o
            WHERE o.status IN :statuses
              AND o.createdAt < :before
            """)
    List<Order> findStaleOrders(@Param("statuses") List<OrderStatus> statuses,
                                @Param("before") Instant before);
}
