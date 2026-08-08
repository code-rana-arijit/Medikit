package com.medikit.health.service.assistant;

import java.util.List;

public interface HealthAssistantComposer {

    boolean isActive();

    String compose(AssistantAnalysis analysis);

    List<String> references(AssistantAnalysis analysis);
}
