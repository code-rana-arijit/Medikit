package com.medikit.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.event.EventPublisher;
import com.medikit.common.event.Topics;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.inventory.dto.StockLevelResponse;
import com.medikit.inventory.dto.StockUpdateRequest;
import com.medikit.inventory.entity.InventoryItem;
import com.medikit.inventory.repository.InventoryRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InventoryService {

    private static final String STOCK_CACHE_KEY_PREFIX = "medikit:inventory:stock:";
    private static final String STOCK_LOCK_KEY_PREFIX = "medikit:inventory:lock:";
    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final int MAX_LOCK_ATTEMPTS = 5;

    private final InventoryRepository inventoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public InventoryService(InventoryRepository inventoryRepository,
                            StringRedisTemplate redisTemplate,
                            EventPublisher eventPublisher,
                            ObjectMapper objectMapper) {
        this.inventoryRepository = inventoryRepository;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    public StockLevelResponse getStock(UUID productId, UUID pharmacyId) {
        String cacheKey = stockCacheKey(productId, pharmacyId);
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                try {
                    return objectMapper.readValue(cached, StockLevelResponse.class);
                } catch (Exception e) {
                    // fall through to database on cache parse failure
                }
            }
        } catch (Exception e) {
            // fall through to database on redis failure
        }

        InventoryItem item = inventoryRepository.findByProductIdAndPharmacyId(productId, pharmacyId)
                .orElseThrow(() -> new NotFoundException("Stock not found for product: " + productId
                        + " at pharmacy: " + pharmacyId));
        StockLevelResponse response = StockLevelResponse.from(item);
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            // Non-blocking cache write failure
        }
        return response;
    }

    public List<StockLevelResponse> getStockBulk(List<UUID> productIds, UUID pharmacyId) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return inventoryRepository.findByProductIdInAndPharmacyId(productIds, pharmacyId)
                .stream()
                .map(StockLevelResponse::from)
                .toList();
    }

    @Transactional
    public StockLevelResponse updateStock(StockUpdateRequest request) {
        String lockKey = stockLockKey(request.productId(), request.pharmacyId());
        for (int attempt = 0; attempt < MAX_LOCK_ATTEMPTS; attempt++) {
            if (acquireLock(lockKey)) {
                try {
                    return doUpdate(request);
                } finally {
                    redisTemplate.delete(lockKey);
                }
            }
            try {
                Thread.sleep(50L * (attempt + 1));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ConflictException("Interrupted while waiting for inventory lock");
            }
        }
        throw new ConflictException("Could not acquire inventory lock, please retry");
    }

    private StockLevelResponse doUpdate(StockUpdateRequest request) {
        InventoryItem item = inventoryRepository.findByProductIdAndPharmacyId(request.productId(), request.pharmacyId())
                .orElseGet(() -> InventoryItem.builder()
                        .productId(request.productId())
                        .pharmacyId(request.pharmacyId())
                        .quantityAvailable(0)
                        .reservedQuantity(0)
                        .minStockLevel(request.minStockLevelOrDefault())
                        .maxStockLevel(request.maxStockLevelOrDefault())
                        .active(request.activeOrDefault())
                        .build());
        item.setQuantityAvailable(request.quantityAvailable());
        item.setMinStockLevel(request.minStockLevelOrDefault());
        item.setMaxStockLevel(request.maxStockLevelOrDefault());
        item.setActive(request.activeOrDefault());

        InventoryItem saved = inventoryRepository.save(item);
        redisTemplate.delete(stockCacheKey(saved.getProductId(), saved.getPharmacyId()));
        publishStockUpdated(saved);
        return StockLevelResponse.from(saved);
    }

    private void publishStockUpdated(InventoryItem item) {
        try {
            eventPublisher.publish(Topics.STOCK_UPDATED, item.getProductId().toString(),
                    Map.of("productId", item.getProductId(),
                            "pharmacyId", item.getPharmacyId(),
                            "quantityAvailable", item.getQuantityAvailable(),
                            "active", item.isActive()));
        } catch (Exception e) {
            // Non-blocking event publish failure
        }
    }

    private boolean acquireLock(String key) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, UUID.randomUUID().toString(), LOCK_TTL));
    }

    private String stockCacheKey(UUID productId, UUID pharmacyId) {
        return STOCK_CACHE_KEY_PREFIX + productId + ":" + pharmacyId;
    }

    private String stockLockKey(UUID productId, UUID pharmacyId) {
        return STOCK_LOCK_KEY_PREFIX + productId + ":" + pharmacyId;
    }
}
