package com.medikit.health.service.assistant;

import com.medikit.health.dto.ConditionMatchDto;
import com.medikit.health.dto.ExtractedDrugDto;
import com.medikit.health.dto.InteractionDto;
import com.medikit.health.model.InteractionSeverity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RuleBasedHealthAssistantComposer implements HealthAssistantComposer {

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String compose(AssistantAnalysis analysis) {
        return switch (analysis.intent()) {
            case GENERAL_ASSIST -> generalAssist();
            case SYMPTOM_ANALYSIS -> composeSymptomAnalysis(analysis);
            case DRUG_INTERACTION -> composeInteractionSummary(analysis);
            case PRESCRIPTION_ANALYSIS -> composePrescriptionSummary(analysis);
            case COMBINED_ANALYSIS -> composeCombined(analysis);
        };
    }

    @Override
    public List<String> references(AssistantAnalysis analysis) {
        return switch (analysis.intent()) {
            case GENERAL_ASSIST -> List.of();
            case SYMPTOM_ANALYSIS -> analysis.conditions().stream()
                    .map(c -> c.condition() + " (score " + c.score() + ")")
                    .collect(Collectors.toList());
            case DRUG_INTERACTION -> analysis.interactions().stream()
                    .map(i -> i.drugA() + " <-> " + i.drugB() + " [" + i.severity() + "]")
                    .collect(Collectors.toList());
            case PRESCRIPTION_ANALYSIS -> analysis.extractedDrugs().stream()
                    .map(d -> d.rawTerm() + " -> " + d.canonicalName())
                    .collect(Collectors.toList());
            case COMBINED_ANALYSIS -> combinedReferences(analysis);
        };
    }

    private String generalAssist() {
        return "I can help with three kinds of health questions: "
                + "(1) symptoms - describe how you feel (e.g. fever, headache) and I will suggest "
                + "likely conditions and appropriate medicines; "
                + "(2) drug interactions - tell me which medicines you take and I will check for "
                + "unsafe combinations; "
                + "(3) prescriptions - paste prescription text or a medicine list and I will extract "
                + "the drugs, cross-check against your order, and flag interactions.";
    }

    private String composeSymptomAnalysis(AssistantAnalysis analysis) {
        if (analysis.conditions().isEmpty()) {
            return "I could not match your symptoms to a known condition. "
                    + "Please include common symptoms such as fever, cough, headache, nausea, "
                    + "abdominal pain, or diarrhea, and consult a doctor if symptoms persist.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Based on the symptoms you reported (").append(String.join(", ", analysis.symptoms()))
                .append("), the most likely conditions are:");
        analysis.conditions().forEach(c -> sb.append("\n- ")
                .append(c.condition()).append(" (confidence ").append(c.score()).append(")"));
        ConditionMatchDto top = analysis.conditions().get(0);
        if (!top.remedies().isEmpty()) {
            sb.append("\nRecommended options for ").append(top.condition()).append(":");
            top.remedies().forEach(r -> sb.append("\n  * ")
                    .append(r.medicine())
                    .append(r.otc() ? " (OTC)" : " (prescription)")
                    .append(" - ").append(r.usageNote()));
        }
        if (analysis.hasCriticalInteraction()) {
            sb.append("\nURGENT: One or more matched conditions require prompt medical attention.");
        }
        return sb.toString();
    }

    private String composeInteractionSummary(AssistantAnalysis analysis) {
        if (analysis.interactions().isEmpty()) {
            return "No known interactions were found between the medicines you mentioned. "
                    + "Always check with your pharmacist, especially for new combinations.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("I found ").append(analysis.interactions().size())
                .append(" interaction(s):");
        analysis.interactions().forEach(i -> sb.append("\n- ")
                .append(i.drugA()).append(" + ").append(i.drugB())
                .append(" [").append(severityLabel(i.severity())).append("]: ")
                .append(i.effect()).append(" Advice: ").append(i.recommendation()));
        if (analysis.hasCriticalInteraction()) {
            sb.append("\nIMPORTANT: A contraindicated or major interaction was detected. "
                    + "Do not combine these medicines without speaking to a doctor or pharmacist.");
        }
        return sb.toString();
    }

    private String composePrescriptionSummary(AssistantAnalysis analysis) {
        if (analysis.extractedDrugs().isEmpty()) {
            return "I could not extract any medicines from the text you provided. "
                    + "Paste the prescription text exactly as written (or with clearer OCR).";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("I extracted ").append(analysis.extractedDrugs().size())
                .append(" medicine(s) from the prescription:");
        analysis.extractedDrugs().forEach(d -> sb.append("\n- ")
                .append(d.rawTerm()).append(" -> ").append(d.canonicalName())
                .append(d.matchedOrderItem() ? " (present in your order)" : " (not in your order)"));
        if (!analysis.interactions().isEmpty()) {
            sb.append("\nInteraction check:");
            analysis.interactions().forEach(i -> sb.append("\n  - ")
                    .append(i.drugA()).append(" + ").append(i.drugB())
                    .append(" [").append(severityLabel(i.severity())).append("]"));
        }
        if (!analysis.orderDiscrepancies().isEmpty()) {
            sb.append("\nOrder items not found on the prescription: ")
                    .append(String.join(", ", analysis.orderDiscrepancies()));
        }
        if (analysis.hasCriticalInteraction()) {
            sb.append("\nURGENT: A contraindicated or major interaction was detected.");
        }
        return sb.toString();
    }

    private String composeCombined(AssistantAnalysis analysis) {
        String symptomPart = analysis.conditions().isEmpty()
                ? ""
                : "Symptom analysis:\n" + composeSymptomAnalysis(analysis) + "\n\n";
        String interactionPart = analysis.interactions().isEmpty()
                ? "No known interactions were found between the medicines involved."
                : composeInteractionSummary(analysis);
        return symptomPart + "Medication check:\n" + interactionPart;
    }

    private List<String> combinedReferences(AssistantAnalysis analysis) {
        List<String> refs = new ArrayList<>();
        analysis.conditions().stream().limit(2)
                .forEach(c -> refs.add(c.condition() + " (score " + c.score() + ")"));
        analysis.interactions().stream().limit(2)
                .forEach(i -> refs.add(i.drugA() + " <-> " + i.drugB() + " [" + i.severity() + "]"));
        return refs;
    }

    private String severityLabel(InteractionSeverity severity) {
        return severity.name().toLowerCase().replace('_', ' ');
    }
}
