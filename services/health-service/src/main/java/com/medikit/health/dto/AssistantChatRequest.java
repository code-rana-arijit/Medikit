package com.medikit.health.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssistantChatRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 2000, message = "Message is too long")
        String message,
        @Size(max = 50, message = "Up to 50 drugs can be included in context")
        List<@NotBlank String> contextDrugs) {
}
