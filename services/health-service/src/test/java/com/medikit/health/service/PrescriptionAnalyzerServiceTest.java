package com.medikit.health.service;

import com.medikit.health.dto.PrescriptionAnalysisResponse;
import com.medikit.health.entity.DrugInteraction;
import com.medikit.health.entity.DrugSynonym;
import com.medikit.health.model.InteractionSeverity;
import com.medikit.health.repository.ConditionRemedyRepository;
import com.medikit.health.repository.DrugInteractionRepository;
import com.medikit.health.repository.DrugSynonymRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionAnalyzerServiceTest {

    @Mock
    private DrugSynonymRepository synonymRepository;

    @Mock
    private DrugInteractionRepository interactionRepository;

    @Mock
    private ConditionRemedyRepository conditionRemedyRepository;

    @Mock
    private DrugInteractionChecker interactionChecker;

    @InjectMocks
    private PrescriptionAnalyzerService analyzer;

    @Test
    void extractsDrugNamesAndResolvesBrandsToSalts() {
        when(synonymRepository.findAll()).thenReturn(List.of(
                synonym("crocin", "paracetamol"),
                synonym("ecosprin", "acetylsalicylic acid")));
        when(interactionRepository.findAll()).thenReturn(List.of(
                interaction("warfarin", "acetylsalicylic acid", InteractionSeverity.MAJOR)));
        when(conditionRemedyRepository.findAll()).thenReturn(List.of());

        PrescriptionAnalysisResponse response =
                analyzer.analyze("Rx: Crocin 650mg and Ecosprin 75mg daily", List.of("Crocin"));

        assertThat(response.extractedDrugs()).extracting("rawTerm")
                .containsExactlyInAnyOrder("crocin", "ecosprin");
        assertThat(response.extractedDrugs()).extracting("canonicalName")
                .containsExactlyInAnyOrder("paracetamol", "acetylsalicylic acid");
        assertThat(response.extractedDrugs())
                .anyMatch(d -> d.rawTerm().equals("crocin") && d.matchedOrderItem());
    }

    @Test
    void detectsInteractionAmongExtractedDrugs() {
        when(synonymRepository.findAll()).thenReturn(List.of());
        when(interactionRepository.findAll()).thenReturn(List.of(
                interaction("warfarin", "acetylsalicylic acid", InteractionSeverity.CONTRAINDICATED)));
        when(conditionRemedyRepository.findAll()).thenReturn(List.of());
        when(interactionChecker.check(anyList())).thenAnswer(inv -> {
            List<String> drugs = inv.getArgument(0);
            return new com.medikit.health.dto.InteractionCheckResponse(
                    true, 1, drugs, List.of(new com.medikit.health.dto.InteractionDto(
                            "warfarin", "acetylsalicylic acid", InteractionSeverity.CONTRAINDICATED,
                            "bleeding risk", "avoid", "test")));
        });

        PrescriptionAnalysisResponse response =
                analyzer.analyze("take warfarin and acetylsalicylic acid", null);

        assertThat(response.hasCriticalInteraction()).isTrue();
        assertThat(response.interactions()).hasSize(1);
    }

    @Test
    void reportsOrderItemsNotOnPrescriptionAsDiscrepancies() {
        when(synonymRepository.findAll()).thenReturn(List.of());
        when(interactionRepository.findAll()).thenReturn(List.of(
                interaction("warfarin", "acetylsalicylic acid", InteractionSeverity.MAJOR)));
        when(conditionRemedyRepository.findAll()).thenReturn(List.of());

        PrescriptionAnalysisResponse response =
                analyzer.analyze("warfarin 5mg daily", List.of("warfarin", "metformin"));

        assertThat(response.orderDiscrepancies()).containsExactly("metformin");
    }

    @Test
    void toleratesCaseAndExtraWhitespaceInPrescription() {
        when(synonymRepository.findAll()).thenReturn(List.of());
        when(interactionRepository.findAll()).thenReturn(List.of(
                interaction("warfarin", "acetylsalicylic acid", InteractionSeverity.MAJOR)));
        when(conditionRemedyRepository.findAll()).thenReturn(List.of());

        PrescriptionAnalysisResponse response =
                analyzer.analyze("WARFARIN    5mg\n acetylsalicylic acid 75mg", null);

        assertThat(response.extractedDrugs()).extracting("rawTerm")
                .containsExactlyInAnyOrder("warfarin", "acetylsalicylic acid");
    }

    @Test
    void noDrugsExtractedForUnrecognizedText() {
        when(synonymRepository.findAll()).thenReturn(List.of());
        when(interactionRepository.findAll()).thenReturn(List.of(
                interaction("warfarin", "acetylsalicylic acid", InteractionSeverity.MAJOR)));
        when(conditionRemedyRepository.findAll()).thenReturn(List.of());

        PrescriptionAnalysisResponse response =
                analyzer.analyze("unknown scribbles here", null);

        assertThat(response.extractedDrugs()).isEmpty();
        assertThat(response.interactions()).isEmpty();
        assertThat(response.hasCriticalInteraction()).isFalse();
    }

    @Test
    void doesNotMatchPartialWords() {
        when(synonymRepository.findAll()).thenReturn(List.of());
        when(interactionRepository.findAll()).thenReturn(List.of(
                interaction("warfarin", "acetylsalicylic acid", InteractionSeverity.MAJOR)));
        when(conditionRemedyRepository.findAll()).thenReturn(List.of());

        PrescriptionAnalysisResponse response =
                analyzer.analyze("warfarinic compound is unrelated", null);

        assertThat(response.extractedDrugs()).isEmpty();
    }

    @Test
    void includesPharmacistVerificationDisclaimer() {
        when(synonymRepository.findAll()).thenReturn(List.of());
        when(interactionRepository.findAll()).thenReturn(List.of(
                interaction("warfarin", "acetylsalicylic acid", InteractionSeverity.MAJOR)));
        when(conditionRemedyRepository.findAll()).thenReturn(List.of());

        PrescriptionAnalysisResponse response = analyzer.analyze("warfarin", null);

        assertThat(response.disclaimer()).contains("registered pharmacist");
    }

    private DrugSynonym synonym(String alias, String canonical) {
        return DrugSynonym.builder()
                .alias(alias)
                .canonicalName(canonical)
                .build();
    }

    private DrugInteraction interaction(String a, String b, InteractionSeverity severity) {
        return DrugInteraction.builder()
                .drugA(a)
                .drugB(b)
                .severity(severity)
                .effect("test effect")
                .recommendation("test advice")
                .source("test")
                .build();
    }
}
