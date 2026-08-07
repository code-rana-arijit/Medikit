package com.medikit.health.dto;

import java.util.List;

public record PrescriptionAnalysisResponse(
        List<ExtractedDrugDto> extractedDrugs,
        List<InteractionDto> interactions,
        boolean hasCriticalInteraction,
        List<String> orderDiscrepancies,
        String disclaimer) {
}
