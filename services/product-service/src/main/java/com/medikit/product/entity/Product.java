package com.medikit.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_name", columnList = "name"),
        @Index(name = "idx_products_salt", columnList = "salt_composition"),
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_products_active", columnList = "active")
})
public class Product {

    public enum ProductType {
        MEDICINE, AYURVEDIC, HOMEOPATHY, HEALTH_CARE, VITAMINS, DEVICE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "salt_composition", length = 255)
    private String saltComposition;

    @Column(length = 100)
    private String manufacturer;

    @Column(nullable = false)
    private BigDecimal mrp;

    @Column(nullable = false)
    private BigDecimal sellingPrice;

    @Column(nullable = false)
    @Builder.Default
    private boolean prescriptionRequired = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductType productType;

    @Column(length = 50)
    private String packaging;

    @Column(length = 30)
    private String packSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "pharmacy_id", nullable = false)
    private UUID pharmacyId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private int rating = 0;

    @Column(nullable = false)
    @Builder.Default
    private long ratingCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}
