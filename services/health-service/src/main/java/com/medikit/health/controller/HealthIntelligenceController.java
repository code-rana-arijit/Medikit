package com.medikit.health.controller;

import com.medikit.health.dto.InteractionCheckRequest;
import com.medikit.health.dto.InteractionCheckResponse;
import com.medikit.health.dto.InteractionDto;
import com.medikit.health.dto.PrescriptionAnalyzeRequest;
import com.medikit.health.dto.PrescriptionAnalysisResponse;
import com.medikit.health.dto.RemedyDto;
import com.medikit.health.dto.SymptomAnalysisRequest;
import com.medikit.health.dto.SymptomAnalysisResponse;
import com.medikit.health.entity.ConditionRemedy;
import com.medikit.health.entity.DrugInteraction;
import com.medikit.health.repository.ConditionRemedyRepository;
import com.medikit.health.repository.DrugInteractionRepository;
import com.medikit.health.service.DrugInteractionChecker;
import com.medikit.health.service.PrescriptionAnalyzerService;
import com.medikit.health.service.SymptomRecommenderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/health")
public class HealthIntelligenceController {

    private final DrugInteractionChecker interactionChecker;
    private final DrugInteractionRepository interactionRepository;
    private final SymptomRecommenderService symptomRecommenderService;
    private final ConditionRemedyRepository conditionRemedyRepository;
    private final PrescriptionAnalyzerService prescriptionAnalyzerService;

    public HealthIntelligenceController(DrugInteractionChecker interactionChecker,
                                        DrugInteractionRepository interactionRepository,
                                        SymptomRecommenderService symptomRecommenderService,
                                        ConditionRemedyRepository conditionRemedyRepository,
                                        PrescriptionAnalyzerService prescriptionAnalyzerService) {
        this.interactionChecker = interactionChecker;
        this.interactionRepository = interactionRepository;
        this.symptomRecommenderService = symptomRecommenderService;
        this.conditionRemedyRepository = conditionRemedyRepository;
        this.prescriptionAnalyzerService = prescriptionAnalyzerService;
    }

    @PostMapping("/interactions/check")
    public InteractionCheckResponse checkInteractions(@Valid @RequestBody InteractionCheckRequest request) {
        return interactionChecker.check(request.drugs());
    }

    @GetMapping("/interactions")
    public ResponseEntity<List<InteractionDto>> listInteractions() {
        List<InteractionDto> all = interactionRepository.findAll().stream()
                .map(i -> new InteractionDto(
                        i.getDrugA(), i.getDrugB(), i.getSeverity(),
                        i.getEffect(), i.getRecommendation(), i.getSource()))
                .toList();
        return ResponseEntity.ok(all);
    }

    @PostMapping("/symptoms/analyze")
    public SymptomAnalysisResponse analyzeSymptoms(@Valid @RequestBody SymptomAnalysisRequest request) {
        return symptomRecommenderService.analyze(request.symptoms());
    }

    @GetMapping("/conditions")
    public ResponseEntity<Map<String, List<RemedyDto>>> listConditions() {
        Map<String, List<RemedyDto>> byCondition = conditionRemedyRepository.findAll().stream()
                .sorted(Comparator.comparingInt(ConditionRemedy::getPriority))
                .collect(Collectors.groupingBy(
                        ConditionRemedy::getConditionName,
                        Collectors.mapping(
                                r -> new RemedyDto(r.getMedicine(), r.isOtc(), r.getPriority(), r.getUsageNote()),
                                Collectors.toList())));
        return ResponseEntity.ok(byCondition);
    }

    @PostMapping("/prescriptions/analyze")
    public PrescriptionAnalysisResponse analyzePrescription(
            @Valid @RequestBody PrescriptionAnalyzeRequest request) {
        return prescriptionAnalyzerService.analyze(request.text(), request.orderItems());
    }
}
