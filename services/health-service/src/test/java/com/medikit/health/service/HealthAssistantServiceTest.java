package com.medikit.health.service;

import com.medikit.health.dto.AssistantChatResponse;
import com.medikit.health.entity.DrugInteraction;
import com.medikit.health.entity.SymptomCondition;
import com.medikit.health.model.AssistantIntent;
import com.medikit.health.model.InteractionSeverity;
import com.medikit.health.repository.SymptomConditionRepository;
import com.medikit.health.service.assistant.RuleBasedHealthAssistantComposer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthAssistantServiceTest {

    @Mock
    private SymptomConditionRepository symptomConditionRepository;

    @Mock
    private SymptomRecommenderService symptomRecommenderService;

    @Mock
    private PrescriptionAnalyzerService prescriptionAnalyzerService;

    @Mock
    private DrugInteractionChecker interactionChecker;

    private RuleBasedHealthAssistantComposer composer;

    private HealthAssistantService assistantService;

    @BeforeEach
    void setUp() {
        composer = new RuleBasedHealthAssistantComposer();
        assistantService = new HealthAssistantService(
                symptomConditionRepository, symptomRecommenderService,
                prescriptionAnalyzerService, interactionChecker,
                List.of(composer));
    }

    @Test
    void generalAssistWhenMessageIsUnrecognized() {
        when(symptomConditionRepository.findAll()).thenReturn(List.of());
        when(prescriptionAnalyzerService.analyze(anyString(), isNull()))
                .thenReturn(new com.medikit.health.dto.PrescriptionAnalysisResponse(
                        List.of(), List.of(), false, List.of(), "disclaimer"));

        AssistantChatResponse response = assistantService.chat("hello, what can you do?", null);

        assertThat(response.intent()).isEqualTo(AssistantIntent.GENERAL_ASSIST);
        assertThat(response.summary()).contains("symptoms");
        assertThat(response.summary()).contains("drug interactions");
    }

    @Test
    void symptomIntentWhenSymptomsPresent() {
        when(symptomConditionRepository.findAll()).thenReturn(List.of(
                condition("fever"), condition("headache")));
        when(prescriptionAnalyzerService.analyze(anyString(), isNull()))
                .thenReturn(new com.medikit.health.dto.PrescriptionAnalysisResponse(
                        List.of(), List.of(), false, List.of(), "disclaimer"));
        when(symptomRecommenderService.analyze(anyList()))
                .thenReturn(new com.medikit.health.dto.SymptomAnalysisResponse(
                        false, List.of("fever"), List.of(), "d"));

        AssistantChatResponse response = assistantService.chat("I have fever and headache", null);

        assertThat(response.intent()).isEqualTo(AssistantIntent.SYMPTOM_ANALYSIS);
    }

    @Test
    void interactionIntentWhenDrugsMentioned() {
        when(symptomConditionRepository.findAll()).thenReturn(List.of());
        when(prescriptionAnalyzerService.analyze(anyString(), anyList()))
                .thenReturn(new com.medikit.health.dto.PrescriptionAnalysisResponse(
                        List.of(), List.of(), false, List.of(), "d"));
        when(interactionChecker.check(anyList()))
                .thenReturn(new com.medikit.health.dto.InteractionCheckResponse(
                        true, 1,
                        List.of("warfarin", "aspirin"),
                        List.of(new com.medikit.health.dto.InteractionDto(
                                "warfarin", "aspirin", InteractionSeverity.MAJOR,
                                "bleeding", "avoid", "test"))));

        AssistantChatResponse response =
                assistantService.chat("is it safe to take warfarin and aspirin together?",
                        List.of("warfarin", "aspirin"));

        assertThat(response.intent()).isEqualTo(AssistantIntent.DRUG_INTERACTION);
        assertThat(response.urgentActionRequired()).isTrue();
    }

    @Test
    void prescriptionIntentWithContextDrugs() {
        when(symptomConditionRepository.findAll()).thenReturn(List.of());
        when(prescriptionAnalyzerService.analyze(anyString(), anyList()))
                .thenReturn(new com.medikit.health.dto.PrescriptionAnalysisResponse(
                        List.of(), List.of(), false, List.of(), "d"));
        when(interactionChecker.check(anyList()))
                .thenReturn(new com.medikit.health.dto.InteractionCheckResponse(
                        false, 0, List.of(), List.of()));

        AssistantChatResponse response = assistantService.chat(
                "check my prescription", List.of("Crocin"));

        assertThat(response.intent()).isEqualTo(AssistantIntent.PRESCRIPTION_ANALYSIS);
    }

    @Test
    void summaryContainsDisclaimer() {
        when(symptomConditionRepository.findAll()).thenReturn(List.of());
        when(prescriptionAnalyzerService.analyze(anyString(), isNull()))
                .thenReturn(new com.medikit.health.dto.PrescriptionAnalysisResponse(
                        List.of(), List.of(), false, List.of(), "d"));

        AssistantChatResponse response = assistantService.chat("hi", null);

        assertThat(response.disclaimer()).contains("not a medical diagnosis");
    }

    private SymptomCondition condition(String symptom) {
        return SymptomCondition.builder()
                .symptom(symptom)
                .conditionName("test")
                .weight(0.5)
                .urgent(false)
                .build();
    }
}
