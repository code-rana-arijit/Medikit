package com.medikit.loyalty.dto;

import java.math.BigDecimal;

public record RedeemResponse(
        String code,
        long pointsRedeemed,
        BigDecimal discountAmount,
        long remainingBalance) {
}
