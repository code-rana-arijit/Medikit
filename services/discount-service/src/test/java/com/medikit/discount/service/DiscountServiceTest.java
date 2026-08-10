package com.medikit.discount.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.NotFoundException;
import com.medikit.discount.dto.DiscountCodeResponse;
import com.medikit.discount.dto.IssueDiscountRequest;
import com.medikit.discount.entity.DiscountCode;
import com.medikit.discount.model.DiscountStatus;
import com.medikit.discount.model.DiscountType;
import com.medikit.discount.repository.DiscountCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountCodeRepository repository;

    private DiscountService discountService;

    @Captor
    private ArgumentCaptor<DiscountCode> codeCaptor;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        discountService = new DiscountService(repository);
    }

    @Test
    void issueCreatesActiveCodeWithDefaultExpiry() {
        DiscountCodeResponse response = discountService.issue(userId, new BigDecimal("100"), null);

        assertThat(response.code()).startsWith("MEDIKIT-");
        assertThat(response.discountAmount()).isEqualByComparingTo("100");
        assertThat(response.status()).isEqualTo(DiscountStatus.ACTIVE.name());
        assertThat(response.expiresAt()).isAfter(Instant.now().plus(29, ChronoUnit.DAYS));

        verify(repository).save(any(DiscountCode.class));
    }

    @Test
    void issueHonorsCustomValidityWindow() {
        discountService.issue(userId, new BigDecimal("50"), 10);

        verify(repository).save(codeCaptor.capture());
        DiscountCode saved = codeCaptor.getValue();
        assertThat(saved.getExpiresAt()).isBefore(Instant.now().plus(11, ChronoUnit.DAYS));
    }

    @Test
    void issuePercentageCodeStoresPercentageOnly() {
        IssueDiscountRequest request = new IssueDiscountRequest(
                userId, DiscountType.PERCENTAGE, null, new BigDecimal("10"),
                null, "Spring Sale", false, 30);

        DiscountCodeResponse response = discountService.issue(request);

        assertThat(response.discountType()).isEqualTo(DiscountType.PERCENTAGE.name());
        assertThat(response.percentage()).isEqualByComparingTo("10");
        assertThat(response.discountAmount()).isNull();
        assertThat(response.title()).isEqualTo("Spring Sale");
    }

    @Test
    void validateAcceptsCampaignCodeWithoutOwner() {
        DiscountCode discount = activeCode();
        discount.setUserId(null);
        discount.setCampaignId(UUID.randomUUID());
        discount.setFirstOrderOnly(true);
        when(repository.findByCode("MEDIKIT-ABC")).thenReturn(Optional.of(discount));

        DiscountCodeResponse response = discountService.validate("MEDIKIT-ABC", userId);

        assertThat(response.status()).isEqualTo(DiscountStatus.ACTIVE.name());
        assertThat(response.firstOrderOnly()).isTrue();
    }

    @Test
    void validateReturnsCodeForOwnedActiveCode() {
        DiscountCode discount = activeCode();
        when(repository.findByCode("MEDIKIT-ABC")).thenReturn(Optional.of(discount));

        DiscountCodeResponse response = discountService.validate("MEDIKIT-ABC", userId);

        assertThat(response.code()).isEqualTo("MEDIKIT-ABC");
        assertThat(response.status()).isEqualTo(DiscountStatus.ACTIVE.name());
    }

    @Test
    void validateRejectsUnknownCode() {
        when(repository.findByCode("NOPE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> discountService.validate("NOPE", userId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void validateRejectsForeignCode() {
        DiscountCode discount = activeCode();
        discount.setUserId(UUID.randomUUID());
        when(repository.findByCode("MEDIKIT-ABC")).thenReturn(Optional.of(discount));

        assertThatThrownBy(() -> discountService.validate("MEDIKIT-ABC", userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void validateRejectsExpiredCode() {
        DiscountCode discount = activeCode();
        discount.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(repository.findByCode("MEDIKIT-ABC")).thenReturn(Optional.of(discount));

        assertThatThrownBy(() -> discountService.validate("MEDIKIT-ABC", userId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void redeemMarksCodeUsedWithOrderId() {
        DiscountCode discount = activeCode();
        when(repository.findByCode("MEDIKIT-ABC")).thenReturn(Optional.of(discount));
        UUID orderId = UUID.randomUUID();

        DiscountCodeResponse response = discountService.redeem("MEDIKIT-ABC", userId, orderId);

        assertThat(response.status()).isEqualTo(DiscountStatus.USED.name());
        assertThat(response.redeemedOrderId()).isEqualTo(orderId);
        verify(repository).save(codeCaptor.capture());
        assertThat(codeCaptor.getValue().getStatus()).isEqualTo(DiscountStatus.USED);
        assertThat(codeCaptor.getValue().getRedeemedAt()).isNotNull();
    }

    @Test
    void redeemRejectsAlreadyUsedCode() {
        DiscountCode discount = activeCode();
        discount.setStatus(DiscountStatus.USED);
        when(repository.findByCode("MEDIKIT-ABC")).thenReturn(Optional.of(discount));

        assertThatThrownBy(() -> discountService.redeem("MEDIKIT-ABC", userId, UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no longer active");
    }

    private DiscountCode activeCode() {
        return DiscountCode.builder()
                .id(1L)
                .code("MEDIKIT-ABC")
                .userId(userId)
                .discountAmount(new BigDecimal("100"))
                .currency("INR")
                .status(DiscountStatus.ACTIVE)
                .expiresAt(Instant.now().plus(30, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .build();
    }
}
