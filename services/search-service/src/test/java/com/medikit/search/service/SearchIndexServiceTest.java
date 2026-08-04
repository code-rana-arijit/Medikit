package com.medikit.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.search.dto.SearchResponse;
import com.medikit.search.model.SearchableProduct;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchIndexServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SearchIndexService searchIndexService;

    @BeforeEach
    void setUp() {
        searchIndexService = new SearchIndexService(redisTemplate, objectMapper);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void search_returnsProductsMatchingQuery() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        SearchableProduct paracetamol = product(
                "p1", "Paracetamol 500mg", "Paracetamol", "Cipla", "ph-1", "10.00", "12.00");
        SearchableProduct dolo = product(
                "p2", "Dolo 650", "Paracetamol", "Micro Labs", "ph-1", "25.00", "30.00");

        when(setOperations.members("medikit:search:token:paracetamol"))
                .thenReturn(Set.of("p1", "p2"));
        when(valueOperations.get("medikit:search:product:p1"))
                .thenReturn(objectMapper.writeValueAsString(paracetamol));
        when(valueOperations.get("medikit:search:product:p2"))
                .thenReturn(objectMapper.writeValueAsString(dolo));

        SearchResponse response = searchIndexService.search("Paracetamol", null, 0, 10);

        assertThat(response.totalHits()).isEqualTo(2);
        assertThat(response.results())
                .extracting(SearchableProduct::getProductId)
                .containsExactlyInAnyOrder("p1", "p2");
    }

    @Test
    void search_filtersByPharmacyId() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        SearchableProduct inPharmacy = product(
                "p1", "Paracetamol 500mg", "Paracetamol", "Cipla", "ph-1", "10.00", "12.00");
        SearchableProduct otherPharmacy = product(
                "p2", "Paracetamol 650", "Paracetamol", "Cipla", "ph-2", "10.00", "12.00");

        when(setOperations.members("medikit:search:token:paracetamol"))
                .thenReturn(Set.of("p1", "p2"));
        when(valueOperations.get("medikit:search:product:p1"))
                .thenReturn(objectMapper.writeValueAsString(inPharmacy));
        when(valueOperations.get("medikit:search:product:p2"))
                .thenReturn(objectMapper.writeValueAsString(otherPharmacy));

        SearchResponse response = searchIndexService.search("paracetamol", "ph-1", 0, 10);

        assertThat(response.totalHits()).isEqualTo(1);
        assertThat(response.results())
                .extracting(SearchableProduct::getProductId)
                .containsExactly("p1");
    }

    @Test
    void search_withoutMatchesReturnsEmptyResponse() {
        when(setOperations.members(anyString())).thenReturn(Set.of());

        SearchResponse response = searchIndexService.search("nonexistent", null, 0, 10);

        assertThat(response.totalHits()).isZero();
        assertThat(response.results()).isEmpty();
    }

    private SearchableProduct product(String productId, String name, String salt,
                                      String manufacturer, String pharmacyId,
                                      String sellingPrice, String mrp) {
        SearchableProduct product = new SearchableProduct();
        product.setProductId(productId);
        product.setName(name);
        product.setSaltComposition(salt);
        product.setManufacturer(manufacturer);
        product.setPharmacyId(pharmacyId);
        product.setSellingPrice(new BigDecimal(sellingPrice));
        product.setMrp(new BigDecimal(mrp));
        product.setPrescriptionRequired(false);
        product.setActive(true);
        product.setSearchTokens(List.of("paracetamol", "500", "cipla", "650"));
        return product;
    }
}
