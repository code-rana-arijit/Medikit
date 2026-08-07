package com.medikit.health.dto;

import java.util.List;

public record SymptomAnalysisResponse(
        boolean urgentActionRequired,
        List<String> normalizedSymptoms,
        List<ConditionMatchDto> conditions,
        String disclaimer) {
}
