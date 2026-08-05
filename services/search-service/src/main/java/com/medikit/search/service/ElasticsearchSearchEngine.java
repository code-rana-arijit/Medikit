package com.medikit.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.medikit.common.web.BadRequestException;
import com.medikit.search.dto.SearchResponse;
import com.medikit.search.model.ProductDocument;
import com.medikit.search.model.SearchableProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Real Elasticsearch-backed full-text search engine.
 * <p>
 * Enabled via `medikit.search.engine=elasticsearch`. The Redis-backed
 * {@link SearchIndexService} remains the default fallback so the service
 * works without an Elasticsearch cluster.
 * </p>
 */
@Service
@ConditionalOnProperty(name = "medikit.search.engine", havingValue = "elasticsearch")
public class ElasticsearchSearchEngine implements SearchEngine {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchSearchEngine.class);

    private static final String INDEX = "medikit-products";

    private final ElasticsearchOperations operations;

    public ElasticsearchSearchEngine(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public void index(SearchableProduct product) {
        if (product == null || product.getProductId() == null || product.getProductId().isBlank()) {
            throw new BadRequestException("Product with a productId is required for indexing");
        }
        operations.save(ProductDocument.from(product), IndexCoordinates.of(INDEX));
        log.debug("Indexed product {} in Elasticsearch", product.getProductId());
    }

    @Override
    public void remove(String productId) {
        if (productId == null || productId.isBlank()) {
            return;
        }
        String result = operations.delete(productId, IndexCoordinates.of(INDEX));
        if (result != null) {
            log.debug("Removed product {} from Elasticsearch", productId);
        }
    }

    @Override
    public void bulkIndex(List<SearchableProduct> products) {
        if (products == null || products.isEmpty()) {
            return;
        }
        List<IndexQuery> queries = products.stream()
                .map(p -> new IndexQueryBuilder()
                        .withId(p.getProductId())
                        .withObject(ProductDocument.from(p))
                        .build())
                .toList();
        operations.bulkIndex(queries, IndexCoordinates.of(INDEX));
        log.debug("Bulk indexed {} products in Elasticsearch", products.size());
    }

    @Override
    public void clear() {
        operations.indexOps(IndexCoordinates.of(INDEX)).delete();
        operations.indexOps(IndexCoordinates.of(INDEX)).create();
        log.debug("Cleared Elasticsearch index {}", INDEX);
    }

    @Override
    public SearchResponse search(String query, String pharmacyId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 10;

        Query q = buildQuery(query, pharmacyId);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize);
        SearchHits<ProductDocument> hits = operations.search(
                new NativeQueryBuilder()
                        .withQuery(q)
                        .withPageable(pageRequest)
                        .build(),
                ProductDocument.class,
                IndexCoordinates.of(INDEX));

        List<SearchableProduct> results = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(ProductDocument::toSearchable)
                .toList();

        return new SearchResponse(query, results, hits.getTotalHits(), safePage, safeSize);
    }

    @Override
    public List<String> suggest(String prefix, int limit) {
        int safeLimit = limit > 0 ? limit : 10;
        if (prefix == null || prefix.isBlank()) {
            return List.of();
        }
        Query q = Builders.prefix(prefix.toLowerCase());
        SearchHits<ProductDocument> hits = operations.search(
                new NativeQueryBuilder()
                        .withQuery(q)
                        .withMaxResults(safeLimit)
                        .build(),
                ProductDocument.class,
                IndexCoordinates.of(INDEX));
        return hits.getSearchHits().stream()
                .map(h -> h.getContent().getName())
                .filter(name -> name != null)
                .distinct()
                .limit(safeLimit)
                .toList();
    }

    private Query buildQuery(String query, String pharmacyId) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        if (query != null && !query.isBlank()) {
            bool.must(Builders.multiMatch(query));
        } else {
            bool.must(Builders.matchAll());
        }
        if (pharmacyId != null && !pharmacyId.isBlank()) {
            bool.filter(Builders.term("pharmacyId", pharmacyId));
        }
        bool.filter(Builders.term("active", true));
        return new Query(bool.build());
    }

    private static final class Builders {
        private static co.elastic.clients.elasticsearch._types.query_dsl.Query multiMatch(String query) {
            return co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.multiMatch(m -> m
                    .query(query)
                    .fields("name^3", "saltComposition^2", "manufacturer^1.5", "searchTokens"));
        }

        private static co.elastic.clients.elasticsearch._types.query_dsl.Query matchAll() {
            return new Query(co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.matchAll().build());
        }

        private static co.elastic.clients.elasticsearch._types.query_dsl.Query term(String field, String value) {
            return co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.term(t -> t.field(field).value(v -> v.stringValue(value)));
        }

        private static co.elastic.clients.elasticsearch._types.query_dsl.Query term(String field, boolean value) {
            return co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.term(t -> t.field(field).value(value));
        }

        private static co.elastic.clients.elasticsearch._types.query_dsl.Query prefix(String prefix) {
            return co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.prefix(t -> t.field("name.keyword").value(prefix));
        }
    }
}
