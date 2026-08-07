package com.medikit.health.dto;

import java.util.List;

public record ConditionMatchDto(
        String condition,
        double score,
        boolean urgent,
        List<String> matchedSymptoms,
        String referralNote,
        List<RemedyDto> remedies) {
}
