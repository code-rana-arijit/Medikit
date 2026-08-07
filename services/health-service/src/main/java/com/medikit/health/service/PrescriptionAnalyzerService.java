package com.medikit.health.service;

import com.medikit.health.dto.ExtractedDrugDto;
import com.medikit.health.dto.PrescriptionAnalysisResponse;
import com.medikit.health.entity.ConditionRemedy;
import com.medikit.health.entity.DrugInteraction;
import com.medikit.health.entity.DrugSynonym;
import com.medikit.health.repository.ConditionRemedyRepository;
import com.medikit.health.repository.DrugInteractionRepository;
import com.medikit.health.repository.DrugSynonymRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PrescriptionAnalyzerService {

    public static final String DISCLAIMER =
            "Prescription text extraction is automated and may misread handwriting or OCR noise. "
                    + "A registered pharmacist must verify the final medicine list before dispensing.";

    private final DrugSynonymRepository synonymRepository;
    private final DrugInteractionRepository interactionRepository;
    private final ConditionRemedyRepository conditionRemedyRepository;
    private final DrugInteractionChecker interactionChecker;

    public PrescriptionAnalyzerService(DrugSynonymRepository synonymRepository,
                                       DrugInteractionRepository interactionRepository,
                                       ConditionRemedyRepository conditionRemedyRepository,
                                       DrugInteractionChecker interactionChecker) {
        this.synonymRepository = synonymRepository;
        this.interactionRepository = interactionRepository;
        this.conditionRemedyRepository = conditionRemedyRepository;
        this.interactionChecker = interactionChecker;
    }

    public PrescriptionAnalysisResponse analyze(String text, List<String> orderItems) {
        String normalizedText = normalize(text);
        Map<String, String> lexicon = buildLexicon();

        List<ExtractedDrugDto> extracted = extract(normalizedText, lexicon, orderItems);

        List<String> canonicalSalts = extracted.stream()
                .map(ExtractedDrugDto::canonicalName)
                .distinct()
                .toList();

        var interactionResponse = canonicalSalts.isEmpty()
                ? null
                : interactionChecker.check(canonicalSalts);

        List<String> discrepancies = findDiscrepancies(orderItems, canonicalSalts, lexicon);

        return new PrescriptionAnalysisResponse(
                extracted,
                interactionResponse == null ? List.of() : interactionResponse.interactions(),
                interactionResponse != null && interactionResponse.hasCriticalInteraction(),
                discrepancies,
                DISCLAIMER);
    }

    private Map<String, String> buildLexicon() {
        Map<String, String> lexicon = new HashMap<>();
        for (DrugSynonym synonym : synonymRepository.findAll()) {
            lexicon.put(normalize(synonym.getAlias()), normalize(synonym.getCanonicalName()));
        }
        for (DrugInteraction interaction : interactionRepository.findAll()) {
            lexicon.put(normalize(interaction.getDrugA()), normalize(interaction.getDrugA()));
            lexicon.put(normalize(interaction.getDrugB()), normalize(interaction.getDrugB()));
        }
        for (ConditionRemedy remedy : conditionRemedyRepository.findAll()) {
            lexicon.put(normalize(remedy.getMedicine()), normalize(remedy.getMedicine()));
        }
        return lexicon;
    }

    private List<ExtractedDrugDto> extract(String normalizedText, Map<String, String> lexicon,
                                           List<String> orderItems) {
        Set<String> orderCanonicals = resolveOrderCanonicals(orderItems, lexicon);
        List<String> terms = lexicon.keySet().stream()
                .filter(t -> !t.isEmpty())
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();

        List<ExtractedDrugDto> extracted = new ArrayList<>();
        boolean[] consumed = new boolean[normalizedText.length()];
        int i = 0;
        while (i < normalizedText.length()) {
            if (consumed[i] || normalizedText.charAt(i) == ' ') {
                i++;
                continue;
            }
            String match = null;
            for (String term : terms) {
                if (term.length() + i > normalizedText.length()) {
                    continue;
                }
                if (normalizedText.startsWith(term, i)
                        && boundaryOk(normalizedText, i, i + term.length())) {
                    match = term;
                    break;
                }
            }
            if (match == null) {
                i++;
                continue;
            }
            int end = i + match.length();
            for (int j = i; j < end; j++) {
                consumed[j] = true;
            }
            String canonical = lexicon.get(match);
            extracted.add(new ExtractedDrugDto(match, canonical, orderCanonicals.contains(canonical)));
            i = end;
        }
        return extracted;
    }

    private boolean boundaryOk(String text, int start, int end) {
        boolean leftOk = start == 0 || text.charAt(start - 1) == ' ';
        boolean rightOk = end == text.length() || text.charAt(end) == ' ';
        return leftOk && rightOk;
    }

    private Set<String> resolveOrderCanonicals(List<String> orderItems, Map<String, String> lexicon) {
        Set<String> canonicals = new HashSet<>();
        if (orderItems == null) {
            return canonicals;
        }
        for (String item : orderItems) {
            String normalized = normalize(item);
            if (!normalized.isEmpty()) {
                canonicals.add(lexicon.getOrDefault(normalized, normalized));
            }
        }
        return canonicals;
    }

    private List<String> findDiscrepancies(List<String> orderItems, List<String> extractedCanonicals,
                                           Map<String, String> lexicon) {
        Set<String> extractedSet = new LinkedHashSet<>(extractedCanonicals);
        List<String> discrepancies = new ArrayList<>();
        if (orderItems == null) {
            return discrepancies;
        }
        for (String item : orderItems) {
            String normalized = normalize(item);
            if (normalized.isEmpty()) {
                continue;
            }
            String canonical = lexicon.getOrDefault(normalized, normalized);
            if (!extractedSet.contains(canonical)) {
                discrepancies.add(canonical);
            }
        }
        return discrepancies;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }
}
