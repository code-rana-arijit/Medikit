package com.medikit.discount.entity;

import com.medikit.discount.model.DiscountStatus;
import com.medikit.discount.model.DiscountType;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "discount_codes",
        indexes = {
                @Index(name = "idx_discount_code_user", columnList = "user_id"),
                @Index(name = "idx_discount_code_status", columnList = "status"),
                @Index(name = "idx_discount_code_campaign", columnList = "campaign_id")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscountCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    @Builder.Default
    private DiscountType discountType = DiscountType.FIXED;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "percentage", precision = 5, scale = 2)
    private BigDecimal percentage;

    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "first_order_only", nullable = false)
    @Builder.Default
    private boolean firstOrderOnly = false;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DiscountStatus status = DiscountStatus.ACTIVE;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "redeemed_at")
    private Instant redeemedAt;

    @Column(name = "redeemed_order_id")
    private UUID redeemedOrderId;
}
