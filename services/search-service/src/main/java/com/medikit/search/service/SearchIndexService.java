package com.medikit.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.web.BadRequestException;
import com.medikit.search.dto.SearchResponse;
import com.medikit.search.model.SearchableProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SearchIndexService implements SearchEngine {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexService.class);

    private static final String PRODUCT_KEY_PREFIX = "medikit:search:product:";
    private static final String TOKENS_KEY = "medikit:search:tokens";
    private static final String TOKEN_KEY_PREFIX = "medikit:search:token:";
    private static final String KEY_PATTERN = "medikit:search:*";
    private static final Pattern WORD_SPLIT = Pattern.compile("[^a-z0-9]+");

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public SearchIndexService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void index(SearchableProduct product) {
        if (product == null || product.getProductId() == null || product.getProductId().isBlank()) {
            throw new BadRequestException("Product with a productId is required for indexing");
        }
        List<String> tokens = buildTokens(product);
        product.setSearchTokens(tokens);

        try {
            redisTemplate.opsForValue().set(productKey(product.getProductId()),
                    objectMapper.writeValueAsString(product));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize product " + product.getProductId(), e);
        }

        SetOperations<String, String> sets = redisTemplate.opsForSet();
        for (String token : tokens) {
            sets.add(TOKENS_KEY, token);
            sets.add(tokenKey(token), product.getProductId());
        }
        log.debug("Indexed product {} with {} tokens", product.getProductId(), tokens.size());
    }

    @Override
    public void remove(String productId) {
        String json = redisTemplate.opsForValue().get(productKey(productId));
        if (json == null) {
            log.debug("Product {} not present in search index, nothing to remove", productId);
            return;
        }
        try {
            SearchableProduct existing = objectMapper.readValue(json, SearchableProduct.class);
            SetOperations<String, String> sets = redisTemplate.opsForSet();
            for (String token : existing.getSearchTokens()) {
                sets.remove(tokenKey(token), productId);
            }
            redisTemplate.delete(productKey(productId));
            log.debug("Removed product {} from search index", productId);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read indexed product " + productId, e);
        }
    }

    @Override
    public void bulkIndex(List<SearchableProduct> products) {
        if (products == null) {
            return;
        }
        products.forEach(this::index);
    }

    @Override
    public void clear() {
        Set<String> keys = redisTemplate.keys(KEY_PATTERN);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("Cleared {} search index keys", keys.size());
        }
    }

    @Override
    public SearchResponse search(String query, String pharmacyId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 10;

        List<String> tokens = tokenize(query);
        if (tokens.isEmpty()) {
            return new SearchResponse(query, List.of(), 0, safePage, safeSize);
        }

        Set<String> ids = productIdsFor(tokens);
        if (ids.isEmpty()) {
            return new SearchResponse(query, List.of(), 0, safePage, safeSize);
        }

        List<SearchableProduct> matches = new ArrayList<>();
        for (String id : ids) {
            String json = redisTemplate.opsForValue().get(productKey(id));
            if (json == null) {
                continue;
            }
            try {
                SearchableProduct product = objectMapper.readValue(json, SearchableProduct.class);
                if (pharmacyId != null && !pharmacyId.isBlank() && !pharmacyId.equals(product.getPharmacyId())) {
                    continue;
                }
                matches.add(product);
            } catch (Exception e) {
                log.warn("Skipping malformed search index entry for product {}", id, e);
            }
        }

        matches.sort(Comparator.comparingInt((SearchableProduct p) -> relevance(p, tokens))
                .reversed()
                .thenComparing(p -> p.getName() == null ? "" : p.getName().toLowerCase(Locale.ROOT)));

        int totalHits = matches.size();
        int from = Math.min(safePage * safeSize, matches.size());
        int to = Math.min(from + safeSize, matches.size());
        List<SearchableProduct> results = from >= to ? List.of() : matches.subList(from, to);

        return new SearchResponse(query, results, totalHits, safePage, safeSize);
    }

    @Override
    public List<String> suggest(String prefix, int limit) {
        String safePrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        int safeLimit = limit > 0 ? limit : 10;
        Set<String> tokens = redisTemplate.opsForSet().members(TOKENS_KEY);
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        return tokens.stream()
                .filter(token -> token.startsWith(safePrefix))
                .sorted()
                .limit(safeLimit)
                .toList();
    }

    private Set<String> productIdsFor(List<String> tokens) {
        SetOperations<String, String> sets = redisTemplate.opsForSet();
        Set<String> ids = sets.members(tokenKey(tokens.get(0)));
        if (ids == null) {
            return Set.of();
        }
        Set<String> intersection = new LinkedHashSet<>(ids);
        for (int i = 1; i < tokens.size(); i++) {
            Set<String> next = sets.members(tokenKey(tokens.get(i)));
            if (next == null || next.isEmpty()) {
                return Set.of();
            }
            intersection.retainAll(next);
            if (intersection.isEmpty()) {
                return Set.of();
            }
        }
        return intersection;
    }

    private int relevance(SearchableProduct product, List<String> tokens) {
        int score = 0;
        for (String token : tokens) {
            if (product.getSearchTokens().contains(token)) {
                score++;
            }
        }
        return score;
    }

    private List<String> buildTokens(SearchableProduct product) {
        Set<String> tokens = new LinkedHashSet<>();
        tokens.addAll(tokenize(product.getName()));
        tokens.addAll(tokenize(product.getSaltComposition()));
        tokens.addAll(tokenize(product.getManufacturer()));
        return new ArrayList<>(tokens);
    }

    private List<String> tokenize(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(WORD_SPLIT.split(value.toLowerCase(Locale.ROOT))).stream()
                .filter(token -> !token.isEmpty())
                .toList();
    }

    private String productKey(String productId) {
        return PRODUCT_KEY_PREFIX + productId;
    }

    private String tokenKey(String token) {
        return TOKEN_KEY_PREFIX + token;
    }
}
