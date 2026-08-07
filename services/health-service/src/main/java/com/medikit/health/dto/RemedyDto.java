package com.medikit.health.dto;

public record RemedyDto(
        String medicine,
        boolean otc,
        int priority,
        String usageNote) {
}
