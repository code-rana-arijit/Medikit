package com.medikit.distributor.repository;

import com.medikit.distributor.entity.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CatalogItemRepository extends JpaRepository<CatalogItem, UUID> {

    List<CatalogItem> findByDistributorIdOrderByProductNameAsc(UUID distributorId);

    Optional<CatalogItem> findByDistributorIdAndId(UUID distributorId, UUID id);

    Optional<CatalogItem> findByDistributorIdAndProductId(UUID distributorId, UUID productId);
}
