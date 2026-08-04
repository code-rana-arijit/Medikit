package com.medikit.search.controller;

import com.medikit.common.web.BadRequestException;
import com.medikit.search.dto.SearchRequest;
import com.medikit.search.dto.SearchResponse;
import com.medikit.search.model.SearchableProduct;
import com.medikit.search.service.SearchEngine;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchEngine searchEngine;

    public SearchController(SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    @GetMapping("/products")
    public ResponseEntity<SearchResponse> search(
            @RequestParam String q,
            @RequestParam(required = false) String pharmacyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        SearchRequest request = new SearchRequest(q, pharmacyId, page, size);
        return ResponseEntity.ok(searchEngine.search(
                request.query(), request.pharmacyId(), request.page(), request.size()));
    }

    @GetMapping("/suggest")
    public ResponseEntity<List<String>> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(searchEngine.suggest(q, limit));
    }

    @PostMapping("/index")
    public ResponseEntity<Void> index(@RequestBody SearchableProduct product) {
        if (product == null || product.getProductId() == null || product.getProductId().isBlank()) {
            throw new BadRequestException("Product with a productId is required for indexing");
        }
        searchEngine.index(product);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/index/bulk")
    public ResponseEntity<Void> bulkIndex(@RequestBody List<SearchableProduct> products) {
        searchEngine.bulkIndex(products);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/index/{productId}")
    public ResponseEntity<Void> remove(@PathVariable String productId) {
        searchEngine.remove(productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/index")
    public ResponseEntity<Void> clear() {
        searchEngine.clear();
        return ResponseEntity.noContent().build();
    }
}
