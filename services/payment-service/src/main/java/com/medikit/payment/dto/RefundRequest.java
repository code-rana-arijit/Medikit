package com.medikit.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundRequest(
        UUID orderId,
        BigDecimal amount,
        String reason
) {
}
