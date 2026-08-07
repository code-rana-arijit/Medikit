package com.medikit.health.config;

import com.medikit.health.entity.DrugInteraction;
import com.medikit.health.entity.DrugSynonym;
import com.medikit.health.model.InteractionSeverity;
import com.medikit.health.repository.DrugInteractionRepository;
import com.medikit.health.repository.DrugSynonymRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Component
@Order(1)
public class DrugInteractionDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DrugInteractionDataSeeder.class);

    private final DrugInteractionRepository interactionRepository;
    private final DrugSynonymRepository synonymRepository;

    public DrugInteractionDataSeeder(DrugInteractionRepository interactionRepository,
                                     DrugSynonymRepository synonymRepository) {
        this.interactionRepository = interactionRepository;
        this.synonymRepository = synonymRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedSynonyms();
        seedInteractions();
    }

    private void seedSynonyms() {
        if (synonymRepository.count() > 0) {
            return;
        }
        List<DrugSynonym> synonyms = List.of(
                synonym("Crocin", "paracetamol"),
                synonym("Dolo", "paracetamol"),
                synonym("Calpol", "paracetamol"),
                synonym("Tylenol", "paracetamol"),
                synonym("Advil", "ibuprofen"),
                synonym("Brufen", "ibuprofen"),
                synonym("Nurofen", "ibuprofen"),
                synonym("Aspirin", "acetylsalicylic acid"),
                synonym("Ecosprin", "acetylsalicylic acid"),
                synonym("Disprin", "acetylsalicylic acid"),
                synonym("Metformin", "metformin"),
                synonym("Glucophage", "metformin"),
                synonym("Warfarin", "warfarin"),
                synonym("Coumadin", "warfarin"),
                synonym("Lipitor", "atorvastatin"),
                synonym("Atorva", "atorvastatin"),
                synonym("Crestor", "rosuvastatin"),
                synonym("Cardace", "ramipril"),
                synonym("Coveram", "perindopril"),
                synonym("Ciplex", "citalopram"),
                synonym("Cipramil", "citalopram"),
                synonym("Wegovy", "semaglutide"),
                synonym("Ozempic", "semaglutide"),
                synonym("Amoxil", "amoxicillin"),
                synonym("Augmentin", "amoxicillin clavulanate"),
                synonym("Claribid", "clarithromycin"),
                synonym("Klacid", "clarithromycin"),
                synonym("Zithromax", "azithromycin"),
                synonym("Phexin", "cephalexin"),
                synonym("Citizen", "diclofenac"),
                synonym("Voltaren", "diclofenac"),
                synonym("Cataflam", "diclofenac"),
                synonym("Panadol", "paracetamol"));
        synonymRepository.saveAll(synonyms);
        log.info("Seeded {} drug synonyms", synonyms.size());
    }

    private void seedInteractions() {
        if (interactionRepository.count() > 0) {
            return;
        }
        List<DrugInteraction> interactions = List.of(
                interaction("warfarin", "acetylsalicylic acid", InteractionSeverity.MAJOR,
                        "Both thin the blood, substantially increasing bleeding risk (GI bleeding, haemorrhage).",
                        "Avoid combination where possible; if needed together, monitor INR and PT closely.",
                        "MediKit interaction KB v1"),
                interaction("warfarin", "ibuprofen", InteractionSeverity.MAJOR,
                        "NSAIDs can impair platelet function and irritate the GI tract, raising bleeding risk with warfarin.",
                        "Avoid concurrent use; prefer paracetamol for pain. If unavoidable, monitor INR frequently.",
                        "MediKit interaction KB v1"),
                interaction("warfarin", "diclofenac", InteractionSeverity.MAJOR,
                        "Combined anticoagulant + NSAID effect markedly increases risk of serious bleeding.",
                        "Avoid combination; use paracetamol-based analgesia instead.",
                        "MediKit interaction KB v1"),
                interaction("warfarin", "amoxicillin", InteractionSeverity.MODERATE,
                        "Some antibiotics can potentiate warfarin's anticoagulant effect by altering gut flora / vitamin K synthesis.",
                        "Monitor INR during and shortly after antibiotic course; adjust dose as needed.",
                        "MediKit interaction KB v1"),
                interaction("warfarin", "clarithromycin", InteractionSeverity.CONTRAINDICATED,
                        "Clarithromycin inhibits CYP enzymes that clear warfarin, dangerously raising INR and bleeding risk.",
                        "Avoid; choose azithromycin or another antibiotic after specialist review.",
                        "MediKit interaction KB v1"),
                interaction("warfarin", "metformin", InteractionSeverity.MINOR,
                        "Minor interaction; no clinically significant effect expected on anticoagulation.",
                        "No action needed. Continue routine INR monitoring.",
                        "MediKit interaction KB v1"),
                interaction("clarithromycin", "atorvastatin", InteractionSeverity.MAJOR,
                        "Clarithromycin raises statin exposure (CYP3A4 inhibition), increasing myopathy/rhabdomyolysis risk.",
                        "Temporarily suspend statin during therapy or switch to a non-CYP3A4 statin (pravastatin).",
                        "MediKit interaction KB v1"),
                interaction("clarithromycin", "rosuvastatin", InteractionSeverity.MODERATE,
                        "Slightly increased statin exposure; risk of myopathy is lower than with other statins.",
                        "Monitor for muscle pain; consider temporary dose reduction.",
                        "MediKit interaction KB v1"),
                interaction("clarithromycin", "amoxicillin", InteractionSeverity.MINOR,
                        "Redundant antibiotic spectrum overlap; no direct harmful interaction.",
                        "Unlikely to be prescribed together for the same infection; confirm indication.",
                        "MediKit interaction KB v1"),
                interaction("amoxicillin", "azithromycin", InteractionSeverity.MINOR,
                        "Both are antibiotics; overlapping use may increase GI side effects.",
                        "Review the need for dual antibiotic therapy with the prescriber.",
                        "MediKit interaction KB v1"),
                interaction("metformin", "citalopram", InteractionSeverity.MODERATE,
                        "Potential additive serotonergic effects; mild risk of serotonin syndrome and hypoglycaemia masking.",
                        "Monitor for dizziness, tremor, sweating; review both prescriptions.",
                        "MediKit interaction KB v1"),
                interaction("metformin", "diclofenac", InteractionSeverity.MODERATE,
                        "NSAIDs can reduce renal function, raising risk of metformin-associated lactic acidosis.",
                        "Avoid in renal impairment; monitor renal function and watch for nausea/weakness.",
                        "MediKit interaction KB v1"),
                interaction("metformin", "semaglutide", InteractionSeverity.MINOR,
                        "Combined effect on gut motility and glucose; additive GI symptoms possible.",
                        "Start low, titrate slowly; monitor for nausea and hypoglycaemia.",
                        "MediKit interaction KB v1"),
                interaction("metformin", "ramipril", InteractionSeverity.MODERATE,
                        "ACE inhibitors may rarely increase metformin sensitivity; combined use is common in diabetes care.",
                        "Routine monitoring of kidney function and blood sugar is sufficient.",
                        "MediKit interaction KB v1"),
                interaction("ramipril", "diclofenac", InteractionSeverity.MAJOR,
                        "NSAIDs blunt ACE-inhibitor blood-pressure effect and can precipitate acute kidney injury.",
                        "Avoid regular NSAID use; prefer paracetamol and monitor BP/kidney function.",
                        "MediKit interaction KB v1"),
                interaction("ramipril", "ibuprofen", InteractionSeverity.MAJOR,
                        "NSAIDs reduce antihypertensive effect and risk acute renal impairment with ACE inhibitors.",
                        "Use short-term lowest dose only if essential; monitor BP and creatinine.",
                        "MediKit interaction KB v1"),
                interaction("ramipril", "spironolactone", InteractionSeverity.MAJOR,
                        "ACE inhibitor + potassium-sparing diuretic can cause life-threatening hyperkalaemia.",
                        "Monitor serum potassium within 1-2 weeks of starting or changing dose.",
                        "MediKit interaction KB v1"),
                interaction("acetylsalicylic acid", "ibuprofen", InteractionSeverity.MODERATE,
                        "Ibuprofen can interfere with aspirin's antiplatelet effect (especially cardioprotective low-dose aspirin).",
                        "Take ibuprofen at least 2 hours after aspirin or choose another analgesic.",
                        "MediKit interaction KB v1"),
                interaction("acetylsalicylic acid", "diclofenac", InteractionSeverity.MODERATE,
                        "Concurrent NSAIDs increase GI bleeding risk without added benefit.",
                        "Avoid combining NSAIDs; use a single agent at the lowest effective dose.",
                        "MediKit interaction KB v1"),
                interaction("paracetamol", "warfarin", InteractionSeverity.MODERATE,
                        "Regular high-dose paracetamol (≥4 g/day for several days) can potentiate warfarin.",
                        "Limit paracetamol to short courses; monitor INR with prolonged use.",
                        "MediKit interaction KB v1"),
                interaction("paracetamol", "citalopram", InteractionSeverity.MINOR,
                        "Minor interaction; no significant clinical consequence.",
                        "No action needed for standard doses.",
                        "MediKit interaction KB v1"),
                interaction("paracetamol", "amoxicillin", InteractionSeverity.MINOR,
                        "No clinically significant interaction.",
                        "Safe to use together.",
                        "MediKit interaction KB v1"),
                interaction("citalopram", "amoxicillin", InteractionSeverity.MINOR,
                        "No significant interaction expected.",
                        "No action needed.",
                        "MediKit interaction KB v1"),
                interaction("citalopram", "azithromycin", InteractionSeverity.MAJOR,
                        "Both can prolong the QT interval, increasing risk of serious arrhythmias (torsades de pointes).",
                        "Avoid combination; prefer amoxicillin/clarithromycin review and an ECG if risk factors.",
                        "MediKit interaction KB v1"),
                interaction("amoxicillin", "metformin", InteractionSeverity.MINOR,
                        "No significant interaction.",
                        "Safe to use together.",
                        "MediKit interaction KB v1"),
                interaction("ibuprofen", "azithromycin", InteractionSeverity.MINOR,
                        "No significant interaction.",
                        "No action needed.",
                        "MediKit interaction KB v1"));
        interactionRepository.saveAll(interactions);
        log.info("Seeded {} drug interactions", interactions.size());
    }

    private DrugSynonym synonym(String alias, String canonical) {
        return DrugSynonym.builder()
                .alias(alias.toLowerCase(Locale.ROOT))
                .canonicalName(canonical.toLowerCase(Locale.ROOT))
                .build();
    }

    private DrugInteraction interaction(String drugA, String drugB, InteractionSeverity severity,
                                        String effect, String recommendation, String source) {
        return DrugInteraction.builder()
                .drugA(drugA)
                .drugB(drugB)
                .severity(severity)
                .effect(effect)
                .recommendation(recommendation)
                .source(source)
                .build();
    }
}
