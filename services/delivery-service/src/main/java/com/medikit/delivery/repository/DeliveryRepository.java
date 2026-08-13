package com.medikit.delivery.repository;

import com.medikit.delivery.entity.Delivery;
import com.medikit.delivery.entity.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByOrderId(UUID orderId);

    List<Delivery> findByPartnerId(UUID partnerId);

    List<Delivery> findByPartnerIdAndStatusIn(UUID partnerId, List<DeliveryStatus> statuses);

    List<Delivery> findByPartnerIdIsNullAndStatus(DeliveryStatus status);

    Page<Delivery> findByStatus(DeliveryStatus status, Pageable pageable);

    long countByStatus(DeliveryStatus status);
}
