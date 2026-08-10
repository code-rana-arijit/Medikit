package com.medikit.user.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleUpgradeRequest(
        @NotBlank(message = "Role is required")
        String role
) {
}
