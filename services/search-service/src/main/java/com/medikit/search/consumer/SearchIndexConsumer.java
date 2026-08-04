package com.medikit.search.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.event.Topics;
import com.medikit.search.dto.ProductUpdatedEvent;
import com.medikit.search.model.SearchableProduct;
import com.medikit.search.service.SearchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SearchIndexConsumer {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexConsumer.class);

    private final SearchEngine searchEngine;
    private final ObjectMapper objectMapper;

    public SearchIndexConsumer(SearchEngine searchEngine, ObjectMapper objectMapper) {
        this.searchEngine = searchEngine;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = Topics.PRODUCT_UPDATED, groupId = "${spring.kafka.consumer.group-id}")
    public void onProductUpdated(String message) {
        try {
            ProductUpdatedEvent event = objectMapper.readValue(message, ProductUpdatedEvent.class);
            if (event.productId() == null || event.productId().isBlank()) {
                log.warn("Skipping product updated event without productId: {}", message);
                return;
            }
            if (!event.active()) {
                searchEngine.remove(event.productId());
                log.info("Removed product {} from search index (inactive)", event.productId());
                return;
            }
            SearchableProduct product = new SearchableProduct();
            product.setProductId(event.productId());
            product.setName(event.name());
            product.setSellingPrice(event.sellingPrice());
            product.setMrp(event.mrp());
            product.setPharmacyId(event.pharmacyId());
            product.setPrescriptionRequired(event.prescriptionRequired());
            product.setActive(true);
            searchEngine.index(product);
            log.info("Indexed product {} in search index", event.productId());
        } catch (Exception e) {
            log.error("Failed to process product updated event: {}", message, e);
        }
    }
}
