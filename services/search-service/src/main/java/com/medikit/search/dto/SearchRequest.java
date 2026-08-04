package com.medikit.search.dto;

public record SearchRequest(
        String query,
        String pharmacyId,
        int page,
        int size
) {
}
