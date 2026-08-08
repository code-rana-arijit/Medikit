package com.medikit.loyalty.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.loyalty.config.LoyaltyProperties;
import com.medikit.loyalty.dto.LoyaltyBalanceResponse;
import com.medikit.loyalty.dto.PointsTransactionDto;
import com.medikit.loyalty.dto.RedeemResponse;
import com.medikit.loyalty.entity.LoyaltyAccount;
import com.medikit.loyalty.entity.PointsTransaction;
import com.medikit.loyalty.model.LoyaltyTier;
import com.medikit.loyalty.model.TransactionType;
import com.medikit.loyalty.repository.LoyaltyAccountRepository;
import com.medikit.loyalty.repository.PointsTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class LoyaltyService {

    private final LoyaltyAccountRepository accountRepository;
    private final PointsTransactionRepository transactionRepository;
    private final LoyaltyProperties properties;

    public LoyaltyService(LoyaltyAccountRepository accountRepository,
                          PointsTransactionRepository transactionRepository,
                          LoyaltyProperties properties) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.properties = properties;
    }

    @Transactional
    public void awardPoints(UUID userId, UUID orderId, BigDecimal amount) {
        if (transactionRepository.existsByOrderId(orderId)) {
            return;
        }
        LoyaltyAccount account = getOrCreate(userId);
        LoyaltyTier tier = LoyaltyTier.fromTotalSpend(account.getTotalSpend().add(amount));
        long points = amount.divide(properties.spendPerPoint(), 0, RoundingMode.DOWN)
                .longValue();
        points = Math.round(points * tier.earnMultiplier());

        account.setBalancePoints(account.getBalancePoints() + points);
        account.setLifetimeEarned(account.getLifetimeEarned() + points);
        account.setTotalSpend(account.getTotalSpend().add(amount));
        account.setTier(tier);
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        transactionRepository.save(PointsTransaction.builder()
                .userId(userId)
                .orderId(orderId)
                .type(TransactionType.EARN)
                .points(points)
                .description("Earned for order " + orderId)
                .createdAt(Instant.now())
                .build());
    }

    @Transactional(readOnly = true)
    public LoyaltyBalanceResponse getBalance(UUID userId) {
        LoyaltyAccount account = getOrCreate(userId);
        LoyaltyTier next = nextTier(account.getTier());
        BigDecimal nextThreshold = next == null ? null : next.spendThreshold();
        return new LoyaltyBalanceResponse(
                account.getBalancePoints(),
                account.getLifetimeEarned(),
                account.getTotalSpend(),
                account.getTier(),
                nextThreshold,
                account.getTier().earnMultiplier());
    }

    @Transactional(readOnly = true)
    public Page<PointsTransactionDto> getTransactions(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(tx -> new PointsTransactionDto(
                        tx.getOrderId(), tx.getType(), tx.getPoints(),
                        tx.getDescription(), tx.getCreatedAt()));
    }

    @Transactional
    public RedeemResponse redeem(UUID userId, long points) {
        LoyaltyAccount account = getOrCreate(userId);
        if (points > account.getBalancePoints()) {
            throw new BadRequestException("Insufficient points balance");
        }
        long units = points / properties.pointsPerRedemptionUnit().longValue();
        if (units < 1) {
            throw new BadRequestException("Redeemable points must cover at least one redemption unit");
        }
        long consumed = units * properties.pointsPerRedemptionUnit().longValue();
        BigDecimal discount = properties.redemptionUnitValue()
                .multiply(BigDecimal.valueOf(units));

        account.setBalancePoints(account.getBalancePoints() - consumed);
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        transactionRepository.save(PointsTransaction.builder()
                .userId(userId)
                .type(TransactionType.REDEEM)
                .points(-consumed)
                .description("Redeemed for " + discount + " discount code")
                .createdAt(Instant.now())
                .build());

        return new RedeemResponse(
                generateCode(userId),
                consumed,
                discount,
                account.getBalancePoints());
    }

    @Transactional
    public void adjustPoints(UUID userId, long delta, String reason) {
        LoyaltyAccount account = getOrCreate(userId);
        long newBalance = account.getBalancePoints() + delta;
        if (newBalance < 0) {
            throw new BadRequestException("Adjustment would make balance negative");
        }
        account.setBalancePoints(newBalance);
        if (delta > 0) {
            account.setLifetimeEarned(account.getLifetimeEarned() + delta);
        }
        account.setUpdatedAt(Instant.now());
        accountRepository.save(account);

        transactionRepository.save(PointsTransaction.builder()
                .userId(userId)
                .type(TransactionType.ADJUST)
                .points(delta)
                .description(reason == null || reason.isBlank() ? "Manual adjustment" : reason)
                .createdAt(Instant.now())
                .build());
    }

    private LoyaltyAccount getOrCreate(UUID userId) {
        return accountRepository.findByUserId(userId).orElseGet(() -> {
            LoyaltyAccount created = LoyaltyAccount.create(userId);
            return accountRepository.save(created);
        });
    }

    private LoyaltyTier nextTier(LoyaltyTier current) {
        int nextOrdinal = current.ordinal() + 1;
        if (nextOrdinal >= LoyaltyTier.values().length) {
            return null;
        }
        return LoyaltyTier.values()[nextOrdinal];
    }

    private String generateCode(UUID userId) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "MEDIKIT-" + suffix;
    }
}
