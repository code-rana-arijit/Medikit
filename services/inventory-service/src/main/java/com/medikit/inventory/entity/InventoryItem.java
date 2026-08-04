package com.medikit.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_items", indexes = {
        @Index(name = "idx_inventory_product", columnList = "product_id"),
        @Index(name = "idx_inventory_pharmacy", columnList = "pharmacy_id"),
        @Index(name = "idx_inventory_active", columnList = "active")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_inventory_product_pharmacy", columnNames = {"product_id", "pharmacy_id"})
})
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "pharmacy_id", nullable = false)
    private UUID pharmacyId;

    @Column(name = "quantity_available", nullable = false)
    @Builder.Default
    private int quantityAvailable = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private int reservedQuantity = 0;

    @Column(name = "min_stock_level", nullable = false)
    @Builder.Default
    private int minStockLevel = 0;

    @Column(name = "max_stock_level", nullable = false)
    @Builder.Default
    private int maxStockLevel = 1000;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public int availableQuantity() {
        return quantityAvailable - reservedQuantity;
    }
}
