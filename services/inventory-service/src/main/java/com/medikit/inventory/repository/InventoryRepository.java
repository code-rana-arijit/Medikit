package com.medikit.inventory.repository;

import com.medikit.inventory.entity.InventoryItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<InventoryItem, UUID> {

    Optional<InventoryItem> findByProductIdAndPharmacyId(UUID productId, UUID pharmacyId);

    List<InventoryItem> findByProductIdInAndPharmacyId(Collection<UUID> productIds, UUID pharmacyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.productId IN :productIds AND i.pharmacyId = :pharmacyId")
    List<InventoryItem> findByProductIdInAndPharmacyIdForUpdate(@Param("productIds") Collection<UUID> productIds,
                                                                @Param("pharmacyId") UUID pharmacyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryItem i WHERE i.id = :id")
    Optional<InventoryItem> findByIdForUpdate(@Param("id") UUID id);
}
