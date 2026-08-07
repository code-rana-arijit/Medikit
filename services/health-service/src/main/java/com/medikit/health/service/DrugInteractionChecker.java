package com.medikit.health.service;

import com.medikit.health.dto.InteractionCheckResponse;
import com.medikit.health.dto.InteractionDto;
import com.medikit.health.entity.DrugInteraction;
import com.medikit.health.entity.DrugSynonym;
import com.medikit.health.model.InteractionSeverity;
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
public class DrugInteractionChecker {

    private static final Comparator<InteractionDto> SEVERITY_DESC =
            Comparator.comparingInt((InteractionDto d) -> d.severity().ordinal()).reversed();

    private final DrugInteractionRepository interactionRepository;
    private final DrugSynonymRepository synonymRepository;

    public DrugInteractionChecker(DrugInteractionRepository interactionRepository,
                                  DrugSynonymRepository synonymRepository) {
        this.interactionRepository = interactionRepository;
        this.synonymRepository = synonymRepository;
    }

    public InteractionCheckResponse check(List<String> drugs) {
        List<String> canonical = resolveAliases(normalizeInput(drugs));
        Set<String> drugSet = Set.copyOf(canonical);

        List<InteractionDto> found = new ArrayList<>();
        Set<String> seenPairs = new HashSet<>();

        for (DrugInteraction interaction : interactionRepository
                .findByDrugAInAndDrugBIn(canonical, canonical)) {
            if (!drugSet.contains(interaction.getDrugA().toLowerCase(Locale.ROOT))
                    || !drugSet.contains(interaction.getDrugB().toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (seenPairs.add(pairKey(interaction.getDrugA(), interaction.getDrugB()))) {
                found.add(toDto(interaction));
            }
        }

        found.sort(SEVERITY_DESC);
        boolean critical = found.stream()
                .anyMatch(d -> d.severity() == InteractionSeverity.CONTRAINDICATED
                        || d.severity() == InteractionSeverity.MAJOR);

        return new InteractionCheckResponse(critical, found.size(), canonical, found);
    }

    private List<String> normalizeInput(List<String> drugs) {
        Set<String> unique = new LinkedHashSet<>();
        if (drugs != null) {
            for (String d : drugs) {
                if (d != null && !d.isBlank()) {
                    unique.add(d.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return List.copyOf(unique);
    }

    private List<String> resolveAliases(List<String> normalized) {
        Map<String, String> aliasMap = new HashMap<>();
        for (DrugSynonym synonym : synonymRepository.findByAliasIn(normalized)) {
            aliasMap.put(synonym.getAlias().toLowerCase(Locale.ROOT),
                    synonym.getCanonicalName().toLowerCase(Locale.ROOT));
        }
        Set<String> resolved = new LinkedHashSet<>();
        for (String name : normalized) {
            resolved.add(aliasMap.getOrDefault(name, name));
        }
        return List.copyOf(resolved);
    }

    private InteractionDto toDto(DrugInteraction i) {
        return new InteractionDto(
                i.getDrugA(),
                i.getDrugB(),
                i.getSeverity(),
                i.getEffect(),
                i.getRecommendation(),
                i.getSource());
    }

    private String pairKey(String a, String b) {
        String x = a.toLowerCase(Locale.ROOT);
        String y = b.toLowerCase(Locale.ROOT);
        return x.compareTo(y) <= 0 ? x + "|" + y : y + "|" + x;
    }
}
