package com.medikit.search.dto;

import com.medikit.search.model.SearchableProduct;

import java.util.List;

public record SearchResponse(
        String query,
        List<SearchableProduct> results,
        long totalHits,
        int page,
        int size
) {
}
