package com.medikit.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OtpRequest(
        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9]{10,15}$", message = "Invalid phone number")
        String phone,

        @NotBlank(message = "OTP is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
        String otp
) {
}
