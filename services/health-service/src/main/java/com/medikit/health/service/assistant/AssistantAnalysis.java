package com.medikit.health.service.assistant;

import com.medikit.health.dto.ConditionMatchDto;
import com.medikit.health.dto.ExtractedDrugDto;
import com.medikit.health.dto.InteractionDto;
import com.medikit.health.model.AssistantIntent;

import java.util.ArrayList;
import java.util.List;

public record AssistantAnalysis(
        AssistantIntent intent,
        List<String> symptoms,
        List<ConditionMatchDto> conditions,
        List<ExtractedDrugDto> extractedDrugs,
        List<InteractionDto> interactions,
        boolean hasCriticalInteraction,
        List<String> orderDiscrepancies) {

    public static AssistantAnalysis generalAssist() {
        return new AssistantAnalysis(
                AssistantIntent.GENERAL_ASSIST,
                List.of(), List.of(), List.of(), List.of(), false, List.of());
    }

    public List<String> references() {
        List<String> refs = new ArrayList<>();
        conditions.stream().limit(2)
                .forEach(c -> refs.add(c.condition() + " (score " + c.score() + ")"));
        interactions.stream().limit(2)
                .forEach(i -> refs.add(i.drugA() + " <-> " + i.drugB() + " [" + i.severity() + "]"));
        return refs;
    }
}
