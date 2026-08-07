package com.medikit.health.service;

import com.medikit.health.dto.InteractionCheckResponse;
import com.medikit.health.entity.DrugInteraction;
import com.medikit.health.entity.DrugSynonym;
import com.medikit.health.model.InteractionSeverity;
import com.medikit.health.repository.DrugInteractionRepository;
import com.medikit.health.repository.DrugSynonymRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrugInteractionCheckerTest {

    @Mock
    private DrugInteractionRepository interactionRepository;

    @Mock
    private DrugSynonymRepository synonymRepository;

    @InjectMocks
    private DrugInteractionChecker checker;

    @Test
    void noInteractionsWhenNonePresent() {
        when(interactionRepository.findByDrugAInAndDrugBIn(anyList(), anyList()))
                .thenReturn(List.of());

        InteractionCheckResponse response = checker.check(List.of("paracetamol", "amoxicillin"));

        assertThat(response.totalInteractions()).isZero();
        assertThat(response.hasCriticalInteraction()).isFalse();
    }

    @Test
    void detectsMajorInteractionBetweenWarfarinAndAspirin() {
        DrugInteraction warfarinAspirin = interaction("warfarin", "acetylsalicylic acid",
                InteractionSeverity.MAJOR);
        when(interactionRepository.findByDrugAInAndDrugBIn(anyList(), anyList()))
                .thenReturn(List.of(warfarinAspirin));

        InteractionCheckResponse response = checker.check(List.of("warfarin", "acetylsalicylic acid"));

        assertThat(response.hasCriticalInteraction()).isTrue();
        assertThat(response.totalInteractions()).isEqualTo(1);
        assertThat(response.interactions().get(0).severity()).isEqualTo(InteractionSeverity.MAJOR);
        assertThat(response.normalizedDrugs()).containsExactly("warfarin", "acetylsalicylic acid");
    }

    @Test
    void resolvesBrandAliasesToCanonicalSalts() {
        DrugSynonym alias = DrugSynonym.builder()
                .alias("ecosprin")
                .canonicalName("acetylsalicylic acid")
                .build();
        DrugInteraction interaction = interaction("warfarin", "acetylsalicylic acid",
                InteractionSeverity.CONTRAINDICATED);
        when(synonymRepository.findByAliasIn(anyCollection())).thenReturn(List.of(alias));
        when(interactionRepository.findByDrugAInAndDrugBIn(anyList(), anyList()))
                .thenReturn(List.of(interaction));

        InteractionCheckResponse response = checker.check(List.of("Warfarin", "Ecosprin"));

        assertThat(response.hasCriticalInteraction()).isTrue();
        assertThat(response.normalizedDrugs()).contains("acetylsalicylic acid");
    }

    @Test
    void doesNotFlagDeduplicatedPairTwice() {
        DrugInteraction one = interaction("warfarin", "aspirin", InteractionSeverity.MAJOR);
        DrugInteraction two = interaction("aspirin", "warfarin", InteractionSeverity.MAJOR);
        when(interactionRepository.findByDrugAInAndDrugBIn(anyList(), anyList()))
                .thenReturn(List.of(one, two));

        InteractionCheckResponse response = checker.check(List.of("warfarin", "aspirin", "aspirin"));

        assertThat(response.totalInteractions()).isEqualTo(1);
    }

    @Test
    void ordersBySeverityDescending() {
        when(interactionRepository.findByDrugAInAndDrugBIn(anyList(), anyList()))
                .thenReturn(List.of(
                        interaction("a", "b", InteractionSeverity.MINOR),
                        interaction("a", "c", InteractionSeverity.CONTRAINDICATED)));

        InteractionCheckResponse response = checker.check(List.of("a", "b", "c"));

        assertThat(response.interactions().get(0).severity()).isEqualTo(InteractionSeverity.CONTRAINDICATED);
        assertThat(response.interactions().get(1).severity()).isEqualTo(InteractionSeverity.MINOR);
    }

    @Test
    void blankAndDuplicateInputsIgnored() {
        when(interactionRepository.findByDrugAInAndDrugBIn(anyList(), anyList()))
                .thenReturn(List.of());

        InteractionCheckResponse response = checker.check(List.of("  ", "metformin", "metformin"));

        assertThat(response.totalInteractions()).isZero();
        assertThat(response.normalizedDrugs()).containsExactly("metformin");
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
