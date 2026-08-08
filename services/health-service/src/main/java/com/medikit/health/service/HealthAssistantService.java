package com.medikit.health.service;

import com.medikit.health.dto.AssistantChatRequest;
import com.medikit.health.dto.AssistantChatResponse;
import com.medikit.health.dto.ConditionMatchDto;
import com.medikit.health.dto.ExtractedDrugDto;
import com.medikit.health.dto.InteractionDto;
import com.medikit.health.dto.SymptomAnalysisResponse;
import com.medikit.health.model.AssistantIntent;
import com.medikit.health.repository.SymptomConditionRepository;
import com.medikit.health.service.assistant.AssistantAnalysis;
import com.medikit.health.service.assistant.HealthAssistantComposer;
import com.medikit.health.service.assistant.RuleBasedHealthAssistantComposer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HealthAssistantService {

    private static final Set<String> INTERACTION_WORDS = Set.of(
            "interact", "interaction", "together", "combine", "combination",
            "conflict", "safe", "contraindicated", "with my", "with me");
    private static final Set<String> PRESCRIPTION_MARKERS = Set.of(
            "rx", "prescription", "take", "daily", "mg", "tab", "cap",
            "dose", "dosage", "times a day", "once a day", "twice");

    private final SymptomConditionRepository symptomConditionRepository;
    private final SymptomRecommenderService symptomRecommenderService;
    private final PrescriptionAnalyzerService prescriptionAnalyzerService;
    private final DrugInteractionChecker interactionChecker;
    private final List<HealthAssistantComposer> composers;

    public HealthAssistantService(SymptomConditionRepository symptomConditionRepository,
                                  SymptomRecommenderService symptomRecommenderService,
                                  PrescriptionAnalyzerService prescriptionAnalyzerService,
                                  DrugInteractionChecker interactionChecker,
                                  List<HealthAssistantComposer> composers) {
        this.symptomConditionRepository = symptomConditionRepository;
        this.symptomRecommenderService = symptomRecommenderService;
        this.prescriptionAnalyzerService = prescriptionAnalyzerService;
        this.interactionChecker = interactionChecker;
        this.composers = composers;
    }

    public AssistantChatResponse chat(String message, List<String> contextDrugs) {
        String normalized = normalize(message);
        List<String> detectedSymptoms = detectSymptoms(normalized);

        PrescriptionAnalysis prescriptionAnalysis = runPrescriptionAnalysis(normalized, contextDrugs);
        List<InteractionDto> interactions = runInteractionCheck(prescriptionAnalysis, contextDrugs);
        List<ConditionMatchDto> conditions = runSymptomAnalysis(detectedSymptoms);

        AssistantIntent intent = resolveIntent(normalized, detectedSymptoms,
                prescriptionAnalysis.extractedDrugs().isEmpty() && contextDrugs == null,
                !interactions.isEmpty());

        boolean hasCritical = interactions.stream()
                .anyMatch(i -> i.severity().name().equals("CONTRAINDICATED")
                        || i.severity().name().equals("MAJOR"));

        AssistantAnalysis analysis = new AssistantAnalysis(
                intent,
                detectedSymptoms,
                conditions,
                prescriptionAnalysis.extractedDrugs(),
                interactions,
                hasCritical,
                prescriptionAnalysis.orderDiscrepancies());

        HealthAssistantComposer composer = composers.stream()
                .filter(c -> !(c instanceof RuleBasedHealthAssistantComposer))
                .filter(HealthAssistantComposer::isActive)
                .findFirst()
                .orElseGet(() -> composers.stream()
                        .filter(c -> c instanceof RuleBasedHealthAssistantComposer)
                        .findFirst()
                        .orElseThrow());

        return new AssistantChatResponse(
                intent,
                composer.compose(analysis),
                composer.references(analysis),
                hasCritical,
                disclaimer());
    }

    private List<String> detectSymptoms(String normalized) {
        Set<String> known = symptomConditionRepository.findAll().stream()
                .map(sc -> sc.getSymptom().toLowerCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return known.stream()
                .filter(symptom -> normalized.contains(" " + symptom) || normalized.startsWith(symptom))
                .toList();
    }

    private PrescriptionAnalysis runPrescriptionAnalysis(String normalized, List<String> contextDrugs) {
        var response = prescriptionAnalyzerService.analyze(normalized, contextDrugs);
        return new PrescriptionAnalysis(
                response.extractedDrugs(),
                response.orderDiscrepancies(),
                response.disclaimer());
    }

    private List<InteractionDto> runInteractionCheck(PrescriptionAnalysis prescriptionAnalysis,
                                                     List<String> contextDrugs) {
        List<String> drugs = new ArrayList<>();
        prescriptionAnalysis.extractedDrugs().forEach(d -> drugs.add(d.canonicalName()));
        if (contextDrugs != null) {
            drugs.addAll(contextDrugs);
        }
        if (drugs.isEmpty()) {
            return List.of();
        }
        return interactionChecker.check(drugs).interactions();
    }

    private List<ConditionMatchDto> runSymptomAnalysis(List<String> detectedSymptoms) {
        if (detectedSymptoms.isEmpty()) {
            return List.of();
        }
        SymptomAnalysisResponse response = symptomRecommenderService.analyze(detectedSymptoms);
        return response.conditions();
    }

    private AssistantIntent resolveIntent(String normalized, List<String> symptoms,
                                          boolean noDrugs, boolean hasInteractions) {
        boolean mentionsInteraction = INTERACTION_WORDS.stream().anyMatch(normalized::contains);
        boolean mentionsPrescription = PRESCRIPTION_MARKERS.stream().anyMatch(normalized::contains);

        if (noDrugs) {
            return symptoms.isEmpty() ? AssistantIntent.GENERAL_ASSIST : AssistantIntent.SYMPTOM_ANALYSIS;
        }
        if (!symptoms.isEmpty() && (mentionsInteraction || mentionsPrescription)) {
            return AssistantIntent.COMBINED_ANALYSIS;
        }
        if (mentionsInteraction || hasInteractions) {
            return AssistantIntent.DRUG_INTERACTION;
        }
        if (mentionsPrescription) {
            return AssistantIntent.PRESCRIPTION_ANALYSIS;
        }
        return AssistantIntent.DRUG_INTERACTION;
    }

    private String disclaimer() {
        return "MediKit's assistant provides general health information, not a medical diagnosis. "
                + "Always confirm medicine decisions with a registered pharmacist or doctor.";
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private record PrescriptionAnalysis(List<ExtractedDrugDto> extractedDrugs,
                                        List<String> orderDiscrepancies,
                                        String disclaimer) {
    }
}
