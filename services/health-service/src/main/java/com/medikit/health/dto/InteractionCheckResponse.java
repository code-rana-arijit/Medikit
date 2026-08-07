package com.medikit.health.dto;

import java.util.List;

public record InteractionCheckResponse(
        boolean hasCriticalInteraction,
        int totalInteractions,
        List<String> normalizedDrugs,
        List<InteractionDto> interactions) {
}
