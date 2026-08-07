package com.medikit.health.service;

import com.medikit.health.dto.SymptomAnalysisResponse;
import com.medikit.health.entity.ConditionRemedy;
import com.medikit.health.entity.SymptomCondition;
import com.medikit.health.entity.SymptomSynonym;
import com.medikit.health.repository.ConditionRemedyRepository;
import com.medikit.health.repository.SymptomConditionRepository;
import com.medikit.health.repository.SymptomSynonymRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SymptomRecommenderServiceTest {

    @Mock
    private SymptomConditionRepository symptomConditionRepository;

    @Mock
    private ConditionRemedyRepository conditionRemedyRepository;

    @Mock
    private SymptomSynonymRepository symptomSynonymRepository;

    @InjectMocks
    private SymptomRecommenderService recommender;

    @Test
    void unknownSymptomsReturnNoConditions() {
        when(symptomConditionRepository.findBySymptomIn(anyCollection())).thenReturn(List.of());

        SymptomAnalysisResponse response = recommender.analyze(List.of("unusual symptom xyz"));

        assertThat(response.conditions()).isEmpty();
        assertThat(response.urgentActionRequired()).isFalse();
    }

    @Test
    void ranksConditionsByAccumulatedScore() {
        when(symptomConditionRepository.findBySymptomIn(anyCollection())).thenReturn(List.of(
                condition("fever", "viral fever", 0.8, false),
                condition("body ache", "viral fever", 0.6, false),
                condition("headache", "migraine", 0.6, false)));

        SymptomAnalysisResponse response = recommender.analyze(List.of("fever", "body ache", "headache"));

        assertThat(response.conditions()).hasSize(2);
        assertThat(response.conditions().get(0).condition()).isEqualTo("viral fever");
        assertThat(response.conditions().get(0).score()).isEqualTo(1.4);
        assertThat(response.conditions().get(1).condition()).isEqualTo("migraine");
    }

    @Test
    void flagsUrgentActionForEmergencySymptoms() {
        when(symptomConditionRepository.findBySymptomIn(anyCollection())).thenReturn(List.of(
                condition("chest pain", "cardiac concern", 1.0, true)));

        SymptomAnalysisResponse response = recommender.analyze(List.of("chest pain"));

        assertThat(response.urgentActionRequired()).isTrue();
        assertThat(response.conditions().get(0).urgent()).isTrue();
    }

    @Test
    void resolvesSymptomAliasesToCanonical() {
        SymptomSynonym alias = SymptomSynonym.builder()
                .alias("loose motion")
                .canonicalSymptom("diarrhea")
                .build();
        when(symptomSynonymRepository.findByAliasIn(anyCollection())).thenReturn(List.of(alias));
        when(symptomConditionRepository.findBySymptomIn(anyCollection())).thenReturn(List.of(
                condition("diarrhea", "gastroenteritis", 0.8, false)));

        SymptomAnalysisResponse response = recommender.analyze(List.of("loose motion"));

        assertThat(response.normalizedSymptoms()).contains("diarrhea");
        assertThat(response.conditions().get(0).condition()).isEqualTo("gastroenteritis");
    }

    @Test
    void attachesRankedRemediesToCondition() {
        when(symptomConditionRepository.findBySymptomIn(anyCollection())).thenReturn(List.of(
                condition("stuffy nose", "common cold", 0.8, false)));
        when(conditionRemedyRepository.findByConditionNameIn(anyCollection())).thenReturn(List.of(
                remedy("common cold", "paracetamol", 1),
                remedy("common cold", "pseudoephedrine", 2)));

        SymptomAnalysisResponse response = recommender.analyze(List.of("stuffy nose"));

        assertThat(response.conditions()).hasSize(1);
        assertThat(response.conditions().get(0).remedies()).hasSize(2);
        assertThat(response.conditions().get(0).remedies().get(0).medicine()).isEqualTo("paracetamol");
        assertThat(response.conditions().get(0).remedies().get(0).otc()).isTrue();
    }

    @Test
    void includesMedicalDisclaimer() {
        when(symptomConditionRepository.findBySymptomIn(anyCollection())).thenReturn(List.of(
                condition("cough", "common cold", 0.5, false)));

        SymptomAnalysisResponse response = recommender.analyze(List.of("cough"));

        assertThat(response.disclaimer()).contains("not a medical diagnosis");
    }

    @Test
    void blankAndDuplicateSymptomsIgnored() {
        when(symptomConditionRepository.findBySymptomIn(anyCollection())).thenReturn(List.of());

        SymptomAnalysisResponse response = recommender.analyze(List.of("  ", "fever", "fever"));

        assertThat(response.normalizedSymptoms()).containsExactly("fever");
    }

    private SymptomCondition condition(String symptom, String conditionName, double weight, boolean urgent) {
        return SymptomCondition.builder()
                .symptom(symptom)
                .conditionName(conditionName)
                .weight(weight)
                .urgent(urgent)
                .referralNote(urgent ? "emergency" : "monitor")
                .build();
    }

    private ConditionRemedy remedy(String condition, String medicine, int priority) {
        return ConditionRemedy.builder()
                .conditionName(condition)
                .medicine(medicine)
                .priority(priority)
                .otc(true)
                .usageNote("test note")
                .build();
    }
}
