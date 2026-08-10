package com.medikit.loyalty.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.loyalty.client.DiscountClient;
import com.medikit.loyalty.config.LoyaltyProperties;
import com.medikit.loyalty.dto.LoyaltyBalanceResponse;
import com.medikit.loyalty.dto.RedeemResponse;
import com.medikit.loyalty.entity.LoyaltyAccount;
import com.medikit.loyalty.model.LoyaltyTier;
import com.medikit.loyalty.model.TransactionType;
import com.medikit.loyalty.repository.LoyaltyAccountRepository;
import com.medikit.loyalty.repository.PointsTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoyaltyServiceTest {

    @Mock
    private LoyaltyAccountRepository accountRepository;

    @Mock
    private PointsTransactionRepository transactionRepository;

    @Mock
    private DiscountClient discountClient;

    private LoyaltyService loyaltyService;

    @Captor
    private ArgumentCaptor<LoyaltyAccount> accountCaptor;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        loyaltyService = new LoyaltyService(accountRepository, transactionRepository, LoyaltyProperties.defaults(), discountClient);
    }

    @Test
    void awardsPointsOnOrderConfirmation() {
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        loyaltyService.awardPoints(userId, UUID.randomUUID(), new BigDecimal("500"));

        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getBalancePoints()).isEqualTo(5);
        assertThat(accountCaptor.getValue().getLifetimeEarned()).isEqualTo(5);
        assertThat(accountCaptor.getValue().getTotalSpend()).isEqualTo(new BigDecimal("500"));
        assertThat(accountCaptor.getValue().getTier()).isEqualTo(LoyaltyTier.BRONZE);
    }

    @Test
    void appliesTierMultiplierForHigherSpenders() {
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        account.setTotalSpend(new BigDecimal("50000"));
        account.setTier(LoyaltyTier.GOLD);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        loyaltyService.awardPoints(userId, UUID.randomUUID(), new BigDecimal("1000"));

        verify(accountRepository).save(accountCaptor.capture());
        // 1000 / 100 = 10 base points * 1.25 multiplier = 12.5 -> 13 (Math.round)
        assertThat(accountCaptor.getValue().getBalancePoints()).isEqualTo(13);
    }

    @Test
    void skipsDuplicateOrderConfirmation() {
        when(transactionRepository.existsByOrderId(any())).thenReturn(true);

        loyaltyService.awardPoints(userId, UUID.randomUUID(), new BigDecimal("500"));

        verify(accountRepository, never()).save(any());
    }

    @Test
    void upgradesTierBasedOnTotalSpend() {
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        account.setTotalSpend(new BigDecimal("4000"));
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        loyaltyService.awardPoints(userId, UUID.randomUUID(), new BigDecimal("1500"));

        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getTier()).isEqualTo(LoyaltyTier.SILVER);
    }

    @Test
    void getBalanceReportsCurrentTierAndMultiplier() {
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        account.setBalancePoints(250);
        account.setLifetimeEarned(1200);
        account.setTotalSpend(new BigDecimal("6000"));
        account.setTier(LoyaltyTier.SILVER);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        LoyaltyBalanceResponse response = loyaltyService.getBalance(userId);

        assertThat(response.balancePoints()).isEqualTo(250);
        assertThat(response.tier()).isEqualTo(LoyaltyTier.SILVER);
        assertThat(response.nextTierThreshold()).isEqualTo(new BigDecimal("25000"));
        assertThat(response.earnMultiplier()).isEqualTo(1.1);
    }

    @Test
    void redeemGeneratesDiscountCodeAndDebitsBalance() {
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        account.setBalancePoints(500);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(discountClient.issue(anyMap())).thenReturn(Map.of("code", "MEDIKIT-12345678"));

        RedeemResponse response = loyaltyService.redeem(userId, 200);

        assertThat(response.code()).isEqualTo("MEDIKIT-12345678");
        assertThat(response.pointsRedeemed()).isEqualTo(200);
        assertThat(response.discountAmount()).isEqualTo(new BigDecimal("20"));
        assertThat(response.remainingBalance()).isEqualTo(300);
        verify(discountClient).issue(anyMap());
        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getBalancePoints()).isEqualTo(300);
    }

    @Test
    void redeemFallsBackToLocalCodeWhenDiscountServiceUnavailable() {
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        account.setBalancePoints(500);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));
        when(discountClient.issue(anyMap())).thenThrow(new RuntimeException("down"));

        RedeemResponse response = loyaltyService.redeem(userId, 200);

        assertThat(response.code()).startsWith("MEDIKIT-");
        assertThat(response.remainingBalance()).isEqualTo(300);
    }

    @Test
    void redeemRejectsInsufficientBalance() {
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        account.setBalancePoints(50);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> loyaltyService.redeem(userId, 200))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient");
    }

    @Test
    void adjustPointsAddsToLifetimeEarnedForPositiveDelta() {
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        loyaltyService.adjustPoints(userId, 150, "bonus");

        verify(accountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getBalancePoints()).isEqualTo(150);
        assertThat(accountCaptor.getValue().getLifetimeEarned()).isEqualTo(150);
    }

    @Test
    void adjustsPointsRecordTransactionType() {
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        account.setBalancePoints(300);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        loyaltyService.adjustPoints(userId, -100, "deduction");

        assertThat(account.getBalancePoints()).isEqualTo(200);
    }

    @Test
    void getReferralCodeReturnsCodeAndUrl() {
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        var response = loyaltyService.getReferralCode(userId);

        assertThat(response.referralCode()).startsWith("REF-");
        assertThat(response.referralUrl()).contains(response.referralCode());
    }

    @Test
    void registerReferralLinksReferredUserToReferrer() {
        LoyaltyAccount referrer = LoyaltyAccount.create(UUID.randomUUID());
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        when(accountRepository.findByReferralCode(referrer.getReferralCode())).thenReturn(Optional.of(referrer));
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        loyaltyService.registerReferral(userId, referrer.getReferralCode());

        assertThat(account.getReferredBy()).isEqualTo(referrer.getUserId());
        verify(accountRepository).save(account);
    }

    @Test
    void registerReferralRejectsSelfReferral() {
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        when(accountRepository.findByReferralCode(account.getReferralCode())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> loyaltyService.registerReferral(userId, account.getReferralCode()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("refer yourself");
    }

    @Test
    void registerReferralRejectsDuplicate() {
        LoyaltyAccount referrer = LoyaltyAccount.create(UUID.randomUUID());
        LoyaltyAccount account = LoyaltyAccount.create(userId);
        account.setReferredBy(referrer.getUserId());
        when(accountRepository.findByReferralCode(referrer.getReferralCode())).thenReturn(Optional.of(referrer));
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> loyaltyService.registerReferral(userId, referrer.getReferralCode()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void awardsReferrerBonusOnReferredUsersFirstOrder() {
        UUID referrerId = UUID.randomUUID();
        LoyaltyAccount referrer = LoyaltyAccount.create(referrerId);
        referrer.setBalancePoints(50);
        LoyaltyAccount referred = LoyaltyAccount.create(userId);
        referred.setReferredBy(referrerId);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(referred));
        when(accountRepository.findByUserId(referrerId)).thenReturn(Optional.of(referrer));

        loyaltyService.awardPoints(userId, UUID.randomUUID(), new BigDecimal("500"));

        assertThat(referrer.getBalancePoints()).isEqualTo(150);
        assertThat(referred.isReferralBonusGranted()).isTrue();
        verify(accountRepository).save(referrer);
    }

    @Test
    void doesNotAwardReferrerBonusTwice() {
        UUID referrerId = UUID.randomUUID();
        LoyaltyAccount referrer = LoyaltyAccount.create(referrerId);
        LoyaltyAccount referred = LoyaltyAccount.create(userId);
        referred.setReferredBy(referrerId);
        referred.setReferralBonusGranted(true);
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(referred));

        loyaltyService.awardPoints(userId, UUID.randomUUID(), new BigDecimal("500"));

        assertThat(referrer.getBalancePoints()).isZero();
        assertThat(referred.isReferralBonusGranted()).isTrue();
    }
}
