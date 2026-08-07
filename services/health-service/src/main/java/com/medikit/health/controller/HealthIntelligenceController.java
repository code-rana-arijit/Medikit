package com.medikit.health.controller;

import com.medikit.health.dto.InteractionCheckRequest;
import com.medikit.health.dto.InteractionCheckResponse;
import com.medikit.health.dto.InteractionDto;
import com.medikit.health.entity.DrugInteraction;
import com.medikit.health.repository.DrugInteractionRepository;
import com.medikit.health.service.DrugInteractionChecker;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/health")
public class HealthIntelligenceController {

    private final DrugInteractionChecker interactionChecker;
    private final DrugInteractionRepository interactionRepository;

    public HealthIntelligenceController(DrugInteractionChecker interactionChecker,
                                        DrugInteractionRepository interactionRepository) {
        this.interactionChecker = interactionChecker;
        this.interactionRepository = interactionRepository;
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
}
