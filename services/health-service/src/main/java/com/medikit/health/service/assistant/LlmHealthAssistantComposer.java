package com.medikit.health.service.assistant;

import com.medikit.health.dto.ConditionMatchDto;
import com.medikit.health.dto.ExtractedDrugDto;
import com.medikit.health.dto.InteractionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class LlmHealthAssistantComposer implements HealthAssistantComposer {

    private static final Logger log = LoggerFactory.getLogger(LlmHealthAssistantComposer.class);

    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final RestClient restClient;

    public LlmHealthAssistantComposer(
            @Value("${medikit.health.assistant.llm.enabled:false}") boolean enabled,
            @Value("${USER_HEALTH_LLM_BASE_URL:}") String baseUrl,
            @Value("${USER_HEALTH_LLM_API_KEY:}") String apiKey,
            @Value("${USER_HEALTH_LLM_MODEL:}") String model) {
        this.enabled = enabled && !baseUrl.isBlank() && !apiKey.isBlank();
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.model = model.isBlank() ? "default" : model;
        this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
    }

    @Override
    public boolean isActive() {
        return enabled;
    }

    @Override
    public String compose(AssistantAnalysis analysis) {
        if (!enabled) {
            throw new IllegalStateException("LLM composer is not enabled");
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0.3,
                    "messages", List.of(
                            Map.of("role", "system",
                                    "content", "You are MediKit's health assistant. Summarize "
                                            + "structured findings into clear, cautious plain-English "
                                            + "advice for a customer. Never diagnose; recommend seeing "
                                            + "a doctor for urgent or persistent symptoms."),
                            Map.of("role", "user", "content", buildPrompt(analysis))));
            LlmResponse response = restClient.post()
                    .uri("/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(LlmResponse.class);
            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                return fallback(analysis);
            }
            return response.choices().get(0).message().content();
        } catch (Exception e) {
            log.warn("LLM summarization failed, falling back to rule-based summary: {}", e.getMessage());
            return fallback(analysis);
        }
    }

    @Override
    public List<String> references(AssistantAnalysis analysis) {
        return analysis.references();
    }

    private String buildPrompt(AssistantAnalysis analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("Intent: ").append(analysis.intent()).append("\n");
        if (!analysis.symptoms().isEmpty()) {
            sb.append("Symptoms: ").append(analysis.symptoms()).append("\n");
        }
        if (!analysis.conditions().isEmpty()) {
            sb.append("Likely conditions:\n");
            for (ConditionMatchDto c : analysis.conditions()) {
                sb.append("- ").append(c.condition()).append(" (score ").append(c.score()).append(")\n");
            }
        }
        if (!analysis.extractedDrugs().isEmpty()) {
            sb.append("Extracted drugs:\n");
            for (ExtractedDrugDto d : analysis.extractedDrugs()) {
                sb.append("- ").append(d.rawTerm()).append(" -> ").append(d.canonicalName()).append("\n");
            }
        }
        if (!analysis.interactions().isEmpty()) {
            sb.append("Interactions:\n");
            for (InteractionDto i : analysis.interactions()) {
                sb.append("- ").append(i.drugA()).append(" + ").append(i.drugB())
                        .append(" [").append(i.severity()).append("]").append("\n");
            }
        }
        if (!analysis.orderDiscrepancies().isEmpty()) {
            sb.append("Order discrepancies: ").append(analysis.orderDiscrepancies()).append("\n");
        }
        if (analysis.hasCriticalInteraction()) {
            sb.append("CRITICAL: contraindicated or major interaction detected.\n");
        }
        return sb.toString();
    }

    private String fallback(AssistantAnalysis analysis) {
        return "Unable to reach the AI summarizer right now. "
                + "Using built-in analysis: "
                + analysis.intent() + " with "
                + analysis.conditions().size() + " condition(s), "
                + analysis.interactions().size() + " interaction(s).";
    }

    record LlmResponse(List<Choice> choices) {
    }

    record Choice(Message message) {
    }

    record Message(String content) {
    }
}
