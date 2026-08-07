package com.medikit.health.config;

import com.medikit.health.entity.ConditionRemedy;
import com.medikit.health.entity.SymptomCondition;
import com.medikit.health.entity.SymptomSynonym;
import com.medikit.health.repository.ConditionRemedyRepository;
import com.medikit.health.repository.SymptomConditionRepository;
import com.medikit.health.repository.SymptomSynonymRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Component
@Order(2)
public class SymptomRecommenderDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SymptomRecommenderDataSeeder.class);

    private final SymptomConditionRepository symptomConditionRepository;
    private final ConditionRemedyRepository conditionRemedyRepository;
    private final SymptomSynonymRepository symptomSynonymRepository;

    public SymptomRecommenderDataSeeder(SymptomConditionRepository symptomConditionRepository,
                                        ConditionRemedyRepository conditionRemedyRepository,
                                        SymptomSynonymRepository symptomSynonymRepository) {
        this.symptomConditionRepository = symptomConditionRepository;
        this.conditionRemedyRepository = conditionRemedyRepository;
        this.symptomSynonymRepository = symptomSynonymRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedSymptomSynonyms();
        seedSymptomConditions();
        seedRemedies();
    }

    private void seedSymptomSynonyms() {
        if (symptomSynonymRepository.count() > 0) {
            return;
        }
        List<SymptomSynonym> synonyms = List.of(
                synonym("feverish", "fever"),
                synonym("high temperature", "fever"),
                synonym("runny nose", "stuffy nose"),
                synonym("blocked nose", "stuffy nose"),
                synonym("sore throat", "throat pain"),
                synonym("tummy ache", "abdominal pain"),
                synonym("loose motion", "diarrhea"),
                synonym("loose stools", "diarrhea"),
                synonym("vomiting", "nausea"),
                synonym("feeling sick", "nausea"),
                synonym("tired all the time", "fatigue"),
                synonym("body pain", "body ache"));
        symptomSynonymRepository.saveAll(synonyms);
        log.info("Seeded {} symptom synonyms", synonyms.size());
    }

    private void seedSymptomConditions() {
        if (symptomConditionRepository.count() > 0) {
            return;
        }
        List<SymptomCondition> conditions = List.of(
                condition("fever", "viral fever", 0.8, false, "Rest, hydrate, monitor temperature for 48 hours."),
                condition("fever", "flu (influenza)", 0.6, false, "Antiviral benefit is highest within 48h of onset; see doctor if high-risk."),
                condition("body ache", "viral fever", 0.6, false, "Rest, hydrate, monitor temperature for 48 hours."),
                condition("body ache", "flu (influenza)", 0.7, false, "Antiviral benefit is highest within 48h of onset; see doctor if high-risk."),
                condition("fatigue", "viral fever", 0.5, false, "Rest, hydrate, monitor temperature for 48 hours."),
                condition("fatigue", "anemia", 0.5, false, "Consider iron studies if fatigue is persistent with paleness."),
                condition("headache", "tension headache", 0.7, false, "Stress management, hydration and sleep usually help."),
                condition("headache", "migraine", 0.6, false, "Triggers, light/sound sensitivity and unilateral pain suggest migraine."),
                condition("nausea", "migraine", 0.5, false, "Triggers, light/sound sensitivity and unilateral pain suggest migraine."),
                condition("throat pain", "viral pharyngitis", 0.8, false, "Usually self-limiting; gargling with warm saline helps."),
                condition("throat pain", "tonsillitis", 0.6, false, "If severe pain with white patches persists, get a strep test."),
                condition("fever", "tonsillitis", 0.4, false, "If severe pain with white patches persists, get a strep test."),
                condition("stuffy nose", "common cold", 0.8, false, "Self-limiting viral illness; rest and fluids."),
                condition("sneezing", "common cold", 0.7, false, "Self-limiting viral illness; rest and fluids."),
                condition("stuffy nose", "allergic rhinitis", 0.6, false, "Allergen avoidance; antihistamines if allergic pattern."),
                condition("sneezing", "allergic rhinitis", 0.8, false, "Allergen avoidance; antihistamines if allergic pattern."),
                condition("abdominal pain", "gastritis", 0.7, false, "Avoid spicy food, alcohol, NSAIDs; eat small meals."),
                condition("abdominal pain", "gastroenteritis", 0.6, false, "ORS rehydration is key; see doctor if blood in stool."),
                condition("diarrhea", "gastroenteritis", 0.8, false, "ORS rehydration is key; see doctor if blood in stool."),
                condition("nausea", "gastritis", 0.5, false, "Avoid spicy food, alcohol, NSAIDs; eat small meals."),
                condition("cough", "common cold", 0.5, false, "Self-limiting viral illness; rest and fluids."),
                condition("cough", "acute bronchitis", 0.6, false, "Cough can last 2-3 weeks; seek care if fever spikes or breathing difficulty."),
                condition("chest pain", "cardiac concern", 1.0, true,
                        "Chest pain may be a medical emergency. Seek emergency care immediately."),
                condition("shortness of breath", "cardiac concern", 0.8, true,
                        "Breathing difficulty may be a medical emergency. Seek emergency care immediately."),
                condition("breathing difficulty", "acute bronchitis", 0.7, false,
                        "Wheezing or breathlessness with cough warrants prompt clinical review."),
                condition("blood in stool", "gastroenteritis", 0.9, true,
                        "Blood in stool requires urgent medical evaluation."));
        symptomConditionRepository.saveAll(conditions);
        log.info("Seeded {} symptom-condition mappings", conditions.size());
    }

    private void seedRemedies() {
        if (conditionRemedyRepository.count() > 0) {
            return;
        }
        List<ConditionRemedy> remedies = List.of(
                remedy("viral fever", "paracetamol", 1, true, "650 mg every 6-8h as needed; max 4 g/day."),
                remedy("viral fever", "ibuprofen", 2, true, "400 mg after food for fever/body ache; avoid with gastritis."),
                remedy("flu (influenza)", "paracetamol", 1, true, "For fever and body ache; max 4 g/day."),
                remedy("flu (influenza)", "ibuprofen", 2, true, "For body ache; take with food."),
                remedy("anemia", "ferrous sulfate", 1, true, "Oral iron on empty stomach with vitamin C; may cause constipation."),
                remedy("tension headache", "paracetamol", 1, true, "Short-term relief; limit to occasional use."),
                remedy("migraine", "paracetamol", 1, true, "Take early in the attack for best effect."),
                remedy("migraine", "ibuprofen", 2, true, "For mild-moderate migraine pain; with food."),
                remedy("viral pharyngitis", "paracetamol", 1, true, "For throat pain and fever."),
                remedy("viral pharyngitis", "throat lozenges", 2, true, "Soothes pain; use as needed."),
                remedy("tonsillitis", "paracetamol", 1, true, "For pain/fever; if severe or persistent, see a doctor."),
                remedy("common cold", "paracetamol", 1, true, "For fever and body aches."),
                remedy("common cold", "pseudoephedrine", 2, true, "For nasal congestion; avoid if hypertensive."),
                remedy("common cold", "dextromethorphan", 3, true, "For dry cough; not for productive cough."),
                remedy("allergic rhinitis", "cetirizine", 1, true, "10 mg once daily for allergy symptoms; may cause drowsiness."),
                remedy("allergic rhinitis", "fluticasone nasal spray", 2, false, "Daily for persistent allergic rhinitis."),
                remedy("gastritis", "antacid", 1, true, "For acid neutralization; take after meals."),
                remedy("gastritis", "omeprazole", 2, false, "PPI for 2-4 weeks; take 30 min before breakfast."),
                remedy("gastroenteritis", "oral rehydration salts", 1, true, "Sip ORS after each loose stool; key for hydration."),
                remedy("gastroenteritis", "loperamide", 2, true, "For watery diarrhea; do not use if fever or blood present."),
                remedy("acute bronchitis", "dextromethorphan", 1, true, "For dry irritating cough."),
                remedy("acute bronchitis", "steam inhalation", 2, true, "Helps loosen mucus; non-drug option."));
        conditionRemedyRepository.saveAll(remedies);
        log.info("Seeded {} condition remedies", remedies.size());
    }

    private SymptomSynonym synonym(String alias, String canonical) {
        return SymptomSynonym.builder()
                .alias(alias.toLowerCase(Locale.ROOT))
                .canonicalSymptom(canonical.toLowerCase(Locale.ROOT))
                .build();
    }

    private SymptomCondition condition(String symptom, String condition, double weight, boolean urgent,
                                       String referralNote) {
        return SymptomCondition.builder()
                .symptom(symptom.toLowerCase(Locale.ROOT))
                .conditionName(condition.toLowerCase(Locale.ROOT))
                .weight(weight)
                .urgent(urgent)
                .referralNote(referralNote)
                .build();
    }

    private ConditionRemedy remedy(String condition, String medicine, int priority, boolean otc, String usageNote) {
        return ConditionRemedy.builder()
                .conditionName(condition.toLowerCase(Locale.ROOT))
                .medicine(medicine)
                .priority(priority)
                .otc(otc)
                .usageNote(usageNote)
                .build();
    }
}
