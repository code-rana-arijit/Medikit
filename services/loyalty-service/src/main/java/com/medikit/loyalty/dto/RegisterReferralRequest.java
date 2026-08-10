package com.medikit.loyalty.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterReferralRequest(
        @NotBlank(message = "Referral code is required")
        String referralCode
) {
}
