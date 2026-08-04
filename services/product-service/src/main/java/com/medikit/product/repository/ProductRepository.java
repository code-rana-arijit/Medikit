package com.medikit.product.repository;

import com.medikit.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByActiveTrueAndCategoryId(UUID categoryId, Pageable pageable);

    Page<Product> findByActiveTrueAndPharmacyId(UUID pharmacyId, Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
              AND (:searchText IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchText, '%'))
                   OR LOWER(p.saltComposition) LIKE LOWER(CONCAT('%', :searchText, '%'))
                   OR LOWER(p.manufacturer) LIKE LOWER(CONCAT('%', :searchText, '%')))
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:minPrice IS NULL OR p.sellingPrice >= :minPrice)
              AND (:maxPrice IS NULL OR p.sellingPrice <= :maxPrice)
              AND (:type IS NULL OR p.productType = :type)
            """)
    Page<Product> search(@Param("searchText") String searchText,
                         @Param("categoryId") UUID categoryId,
                         @Param("minPrice") BigDecimal minPrice,
                         @Param("maxPrice") BigDecimal maxPrice,
                         @Param("type") Product.ProductType type,
                         Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true
            ORDER BY (p.ratingCount * p.rating) DESC
            """)
    Page<Product> findTrending(Pageable pageable);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true AND p.saltComposition = :saltComposition
            """)
    List<Product> findAlternatives(@Param("saltComposition") String saltComposition, Pageable pageable);

    List<Product> findByPharmacyIdInAndActiveTrue(List<UUID> pharmacyIds);
}
