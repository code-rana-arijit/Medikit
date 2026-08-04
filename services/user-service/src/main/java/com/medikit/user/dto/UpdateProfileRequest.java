package com.medikit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 120, message = "Full name must be between 2 and 120 characters")
        String fullName,

        @Email(message = "Invalid email format")
        String email
) {
}
