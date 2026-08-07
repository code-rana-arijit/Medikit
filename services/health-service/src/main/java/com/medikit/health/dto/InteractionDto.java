package com.medikit.health.dto;

import com.medikit.health.model.InteractionSeverity;

public record InteractionDto(
        String drugA,
        String drugB,
        InteractionSeverity severity,
        String effect,
        String recommendation,
        String source) {
}
