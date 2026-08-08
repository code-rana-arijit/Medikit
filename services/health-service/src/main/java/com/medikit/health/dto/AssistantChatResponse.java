package com.medikit.health.dto;

import com.medikit.health.model.AssistantIntent;

import java.util.List;

public record AssistantChatResponse(
        AssistantIntent intent,
        String summary,
        List<String> references,
        boolean urgentActionRequired,
        String disclaimer) {
}
