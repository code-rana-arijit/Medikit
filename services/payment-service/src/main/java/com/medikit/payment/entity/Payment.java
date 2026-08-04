package com.medikit.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_order", columnList = "orderId", unique = true),
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_merchant_ref", columnList = "merchantRefId", unique = true)
})
public class Payment {

    public enum PaymentStatus {
        INITIATED, AUTHORIZED, CAPTURED, COMPLETED, FAILED, REFUNDED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID orderId;

    @Column(nullable = false, unique = true)
    private String merchantRefId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 100)
    private String provider;

    @Column(length = 255)
    private String failureReason;

    @Column(length = 500)
    private String metadata;

    @Column(nullable = false)
    @Builder.Default
    private boolean captured = false;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant capturedAt;
}
