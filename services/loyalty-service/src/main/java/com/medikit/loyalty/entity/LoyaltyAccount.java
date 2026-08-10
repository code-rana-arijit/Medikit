package com.medikit.loyalty.entity;

import com.medikit.loyalty.model.LoyaltyTier;
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
@Table(name = "loyalty_accounts", indexes = {
        @Index(name = "idx_loyalty_account_referral", columnList = "referral_code", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "balance_points", nullable = false)
    private long balancePoints;

    @Column(name = "lifetime_earned", nullable = false)
    private long lifetimeEarned;

    @Column(name = "total_spend", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSpend;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private LoyaltyTier tier;

    @Column(name = "referral_code", length = 24)
    private String referralCode;

    @Column(name = "referred_by")
    private UUID referredBy;

    @Column(name = "referral_bonus_granted", nullable = false)
    @Builder.Default
    private boolean referralBonusGranted = false;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static LoyaltyAccount create(UUID userId) {
        return LoyaltyAccount.builder()
                .userId(userId)
                .balancePoints(0)
                .lifetimeEarned(0)
                .totalSpend(BigDecimal.ZERO)
                .tier(LoyaltyTier.BRONZE)
                .referralCode(generateReferralCode(userId))
                .updatedAt(Instant.now())
                .build();
    }

    private static String generateReferralCode(UUID userId) {
        String suffix = userId.toString().replace("-", "").substring(0, 8).toUpperCase();
        return "REF-" + suffix;
    }
}
