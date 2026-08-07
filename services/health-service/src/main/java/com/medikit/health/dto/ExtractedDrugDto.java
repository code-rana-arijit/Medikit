package com.medikit.health.dto;

public record ExtractedDrugDto(
        String rawTerm,
        String canonicalName,
        boolean matchedOrderItem) {
}
