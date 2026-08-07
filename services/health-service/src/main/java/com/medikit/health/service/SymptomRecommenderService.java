package com.medikit.health.service;

import com.medikit.health.dto.ConditionMatchDto;
import com.medikit.health.dto.RemedyDto;
import com.medikit.health.dto.SymptomAnalysisResponse;
import com.medikit.health.entity.ConditionRemedy;
import com.medikit.health.entity.SymptomCondition;
import com.medikit.health.entity.SymptomSynonym;
import com.medikit.health.repository.ConditionRemedyRepository;
import com.medikit.health.repository.SymptomConditionRepository;
import com.medikit.health.repository.SymptomSynonymRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class SymptomRecommenderService {

    public static final String DISCLAIMER =
            "This is a general health-information suggestion, not a medical diagnosis. "
                    + "Always consult a registered pharmacist or doctor before taking any medicine, "
                    + "especially if symptoms persist, worsen, or if you are pregnant, nursing, or have chronic conditions.";

    private final SymptomConditionRepository symptomConditionRepository;
    private final ConditionRemedyRepository conditionRemedyRepository;
    private final SymptomSynonymRepository symptomSynonymRepository;

    public SymptomRecommenderService(SymptomConditionRepository symptomConditionRepository,
                                     ConditionRemedyRepository conditionRemedyRepository,
                                     SymptomSynonymRepository symptomSynonymRepository) {
        this.symptomConditionRepository = symptomConditionRepository;
        this.conditionRemedyRepository = conditionRemedyRepository;
        this.symptomSynonymRepository = symptomSynonymRepository;
    }

    public SymptomAnalysisResponse analyze(List<String> symptoms) {
        List<String> canonical = resolveAliases(normalizeInput(symptoms));
        List<SymptomCondition> matches = symptomConditionRepository.findBySymptomIn(canonical);

        Map<String, ConditionAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (SymptomCondition match : matches) {
            accumulatorMap
                    .computeIfAbsent(match.getConditionName(), ConditionAccumulator::new)
                    .add(match);
        }

        boolean urgent = accumulatorMap.values().stream().anyMatch(ConditionAccumulator::isUrgent);

        Set<String> matchedConditions = accumulatorMap.keySet();
        Map<String, List<ConditionRemedy>> remediesByCondition = groupRemedies(
                conditionRemedyRepository.findByConditionNameIn(matchedConditions));

        List<ConditionMatchDto> conditions = accumulatorMap.values().stream()
                .sorted(Comparator.comparingDouble(ConditionAccumulator::score).reversed())
                .map(acc -> toDto(acc, remediesByCondition.getOrDefault(acc.conditionName, List.of())))
                .toList();

        return new SymptomAnalysisResponse(urgent, canonical, conditions, DISCLAIMER);
    }

    private List<String> normalizeInput(List<String> symptoms) {
        Set<String> unique = new LinkedHashSet<>();
        if (symptoms != null) {
            for (String s : symptoms) {
                if (s != null && !s.isBlank()) {
                    unique.add(s.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return List.copyOf(unique);
    }

    private List<String> resolveAliases(List<String> normalized) {
        Map<String, String> aliasMap = new HashMap<>();
        for (SymptomSynonym synonym : symptomSynonymRepository.findByAliasIn(normalized)) {
            aliasMap.put(synonym.getAlias().toLowerCase(Locale.ROOT),
                    synonym.getCanonicalSymptom().toLowerCase(Locale.ROOT));
        }
        Set<String> resolved = new LinkedHashSet<>();
        for (String symptom : normalized) {
            resolved.add(aliasMap.getOrDefault(symptom, symptom));
        }
        return List.copyOf(resolved);
    }

    private Map<String, List<ConditionRemedy>> groupRemedies(List<ConditionRemedy> remedies) {
        Map<String, List<ConditionRemedy>> grouped = new HashMap<>();
        for (ConditionRemedy remedy : remedies) {
            grouped.computeIfAbsent(remedy.getConditionName(), k -> new ArrayList<>()).add(remedy);
        }
        return grouped;
    }

    private ConditionMatchDto toDto(ConditionAccumulator acc, List<ConditionRemedy> remedies) {
        List<RemedyDto> remedyDtos = remedies.stream()
                .sorted(Comparator.comparingInt(ConditionRemedy::getPriority))
                .map(r -> new RemedyDto(r.getMedicine(), r.isOtc(), r.getPriority(), r.getUsageNote()))
                .toList();
        return new ConditionMatchDto(
                acc.conditionName,
                acc.score,
                acc.urgent,
                List.copyOf(acc.matchedSymptoms),
                acc.referralNote,
                remedyDtos);
    }

    private static final class ConditionAccumulator {

        private final String conditionName;
        private final Set<String> matchedSymptoms = new LinkedHashSet<>();
        private double score;
        private boolean urgent;
        private String referralNote;

        private ConditionAccumulator(String conditionName) {
            this.conditionName = conditionName;
        }

        private void add(SymptomCondition match) {
            matchedSymptoms.add(match.getSymptom());
            score += match.getWeight();
            if (match.isUrgent()) {
                urgent = true;
                referralNote = match.getReferralNote();
            } else if (referralNote == null) {
                referralNote = match.getReferralNote();
            }
        }

        private double score() {
            return Math.round(score * 100.0) / 100.0;
        }

        private boolean isUrgent() {
            return urgent;
        }
    }
}
