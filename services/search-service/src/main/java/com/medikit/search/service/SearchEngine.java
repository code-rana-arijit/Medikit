package com.medikit.search.service;

import com.medikit.search.dto.SearchResponse;
import com.medikit.search.model.SearchableProduct;

import java.util.List;

public interface SearchEngine {

    void index(SearchableProduct product);

    void remove(String productId);

    void bulkIndex(List<SearchableProduct> products);

    void clear();

    SearchResponse search(String query, String pharmacyId, int page, int size);

    List<String> suggest(String prefix, int limit);
}
