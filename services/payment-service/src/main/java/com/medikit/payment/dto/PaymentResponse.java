package com.medikit.payment.dto;

import com.medikit.payment.entity.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String merchantRefId,
        BigDecimal amount,
        String currency,
        String method,
        String status,
        String provider,
        String failureReason,
        Instant createdAt,
        Instant capturedAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getMerchantRefId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getStatus().name(),
                payment.getProvider(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getCapturedAt());
    }
}
