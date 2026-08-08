package com.medikit.discount.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.NotFoundException;
import com.medikit.discount.dto.DiscountCodeResponse;
import com.medikit.discount.entity.DiscountCode;
import com.medikit.discount.model.DiscountStatus;
import com.medikit.discount.repository.DiscountCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class DiscountService {

    private static final Logger log = LoggerFactory.getLogger(DiscountService.class);
    private static final int DEFAULT_VALID_FOR_DAYS = 30;

    private final DiscountCodeRepository repository;

    public DiscountService(DiscountCodeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DiscountCodeResponse issue(UUID userId, BigDecimal discountAmount, Integer validForDays) {
        int days = validForDays == null || validForDays <= 0 ? DEFAULT_VALID_FOR_DAYS : validForDays;
        DiscountCode code = DiscountCode.builder()
                .code(generateCode())
                .userId(userId)
                .discountAmount(discountAmount)
                .currency("INR")
                .status(DiscountStatus.ACTIVE)
                .expiresAt(Instant.now().plus(days, ChronoUnit.DAYS))
                .createdAt(Instant.now())
                .build();
        repository.save(code);
        log.info("Issued discount code {} for user {} worth {}", code.getCode(), userId, discountAmount);
        return toResponse(code);
    }

    @Transactional(readOnly = true)
    public DiscountCodeResponse validate(String code, UUID userId) {
        return toResponse(resolveValid(code, userId));
    }

    @Transactional
    public DiscountCodeResponse redeem(String code, UUID userId, UUID orderId) {
        DiscountCode discount = resolveValid(code, userId);
        discount.setStatus(DiscountStatus.USED);
        discount.setRedeemedAt(Instant.now());
        discount.setRedeemedOrderId(orderId);
        repository.save(discount);
        log.info("Redeemed discount code {} for order {}", code, orderId);
        return toResponse(discount);
    }

    @Transactional(readOnly = true)
    public Page<DiscountCodeResponse> getMyCodes(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    private DiscountCode resolveValid(String code, UUID userId) {
        DiscountCode discount = repository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Discount code not found"));
        if (!discount.getUserId().equals(userId)) {
            throw new BadRequestException("Discount code does not belong to this user");
        }
        if (discount.getStatus() != DiscountStatus.ACTIVE) {
            throw new BadRequestException("Discount code is no longer active");
        }
        if (discount.getExpiresAt() != null && discount.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Discount code has expired");
        }
        return discount;
    }

    private String generateCode() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "MEDIKIT-" + suffix;
    }

    private DiscountCodeResponse toResponse(DiscountCode discount) {
        return new DiscountCodeResponse(
                discount.getCode(),
                discount.getUserId(),
                discount.getDiscountAmount(),
                discount.getCurrency(),
                discount.getStatus().name(),
                discount.getExpiresAt(),
                discount.getCreatedAt(),
                discount.getRedeemedAt(),
                discount.getRedeemedOrderId());
    }
}
