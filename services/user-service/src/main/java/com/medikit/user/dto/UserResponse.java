package com.medikit.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String phone,
        String fullName,
        String role,
        boolean emailVerified,
        boolean phoneVerified,
        Instant createdAt
) {
}
