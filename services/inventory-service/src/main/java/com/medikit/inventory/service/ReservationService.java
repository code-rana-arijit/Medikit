package com.medikit.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.event.EventPublisher;
import com.medikit.common.event.Topics;
import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.NotFoundException;
import com.medikit.inventory.dto.StockReservationRequest;
import com.medikit.inventory.dto.StockReservationResponse;
import com.medikit.inventory.entity.InventoryItem;
import com.medikit.inventory.repository.InventoryRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final String RESERVATION_KEY_PREFIX = "medikit:inventory:reservation:";
    private static final String STOCK_CACHE_KEY_PREFIX = "medikit:inventory:stock:";
    private static final Duration RESERVATION_TTL = Duration.ofMinutes(30);

    private final InventoryRepository inventoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public ReservationService(InventoryRepository inventoryRepository,
                              StringRedisTemplate redisTemplate,
                              EventPublisher eventPublisher,
                              ObjectMapper objectMapper) {
        this.inventoryRepository = inventoryRepository;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public StockReservationResponse reserveStock(StockReservationRequest request) {
        List<UUID> productIds = request.items().stream().map(StockReservationRequest.Item::productId).toList();
        Map<UUID, InventoryItem> byProduct = inventoryRepository
                .findByProductIdInAndPharmacyIdForUpdate(productIds, request.pharmacyId())
                .stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, i -> i));

        for (StockReservationRequest.Item req : request.items()) {
            InventoryItem item = byProduct.get(req.productId());
            if (item == null || !item.isActive() || item.availableQuantity() < req.quantity()) {
                publishFailure(request.orderId(), "Insufficient stock for product: " + req.productId());
                return new StockReservationResponse(request.orderId(), "FAILED", List.of());
            }
        }

        List<StockReservationResponse.ReservedItem> reserved = new ArrayList<>();
        for (StockReservationRequest.Item req : request.items()) {
            InventoryItem item = byProduct.get(req.productId());
            item.setQuantityAvailable(item.getQuantityAvailable() - req.quantity());
            item.setReservedQuantity(item.getReservedQuantity() + req.quantity());
            reserved.add(new StockReservationResponse.ReservedItem(item.getProductId(), req.quantity(), item.availableQuantity()));
        }
        inventoryRepository.saveAll(byProduct.values());
        byProduct.values().forEach(i -> evictStockCache(i.getProductId(), i.getPharmacyId()));
        saveReservation(request);
        publish(Topics.INVENTORY_RESERVED, request.orderId(),
                Map.of("orderId", request.orderId(), "items", reserved, "status", "RESERVED"));
        return new StockReservationResponse(request.orderId(), "RESERVED", reserved);
    }

    @Transactional
    public StockReservationResponse confirmReservation(UUID orderId) {
        StockReservationRequest request = loadReservation(orderId);
        List<UUID> productIds = request.items().stream().map(StockReservationRequest.Item::productId).toList();
        Map<UUID, InventoryItem> byProduct = inventoryRepository
                .findByProductIdInAndPharmacyIdForUpdate(productIds, request.pharmacyId())
                .stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, i -> i));

        List<StockReservationResponse.ReservedItem> reserved = new ArrayList<>();
        for (StockReservationRequest.Item req : request.items()) {
            InventoryItem item = byProduct.get(req.productId());
            if (item == null) {
                throw new NotFoundException("Inventory item not found for product: " + req.productId());
            }
            item.setReservedQuantity(item.getReservedQuantity() - req.quantity());
            reserved.add(new StockReservationResponse.ReservedItem(item.getProductId(), req.quantity(), item.availableQuantity()));
        }
        inventoryRepository.saveAll(byProduct.values());
        byProduct.values().forEach(i -> evictStockCache(i.getProductId(), i.getPharmacyId()));
        redisTemplate.delete(reservationKey(orderId));
        publish(Topics.INVENTORY_DEDUCTED, orderId,
                Map.of("orderId", orderId, "items", reserved, "status", "DEDUCTED"));
        return new StockReservationResponse(orderId, "DEDUCTED", reserved);
    }

    @Transactional
    public StockReservationResponse releaseReservation(UUID orderId) {
        StockReservationRequest request = loadReservation(orderId);
        List<UUID> productIds = request.items().stream().map(StockReservationRequest.Item::productId).toList();
        Map<UUID, InventoryItem> byProduct = inventoryRepository
                .findByProductIdInAndPharmacyIdForUpdate(productIds, request.pharmacyId())
                .stream()
                .collect(Collectors.toMap(InventoryItem::getProductId, i -> i));

        List<StockReservationResponse.ReservedItem> reserved = new ArrayList<>();
        for (StockReservationRequest.Item req : request.items()) {
            InventoryItem item = byProduct.get(req.productId());
            if (item == null) {
                throw new NotFoundException("Inventory item not found for product: " + req.productId());
            }
            item.setQuantityAvailable(item.getQuantityAvailable() + req.quantity());
            item.setReservedQuantity(item.getReservedQuantity() - req.quantity());
            reserved.add(new StockReservationResponse.ReservedItem(item.getProductId(), req.quantity(), item.availableQuantity()));
        }
        inventoryRepository.saveAll(byProduct.values());
        byProduct.values().forEach(i -> evictStockCache(i.getProductId(), i.getPharmacyId()));
        redisTemplate.delete(reservationKey(orderId));
        publish(Topics.INVENTORY_RELEASED, orderId,
                Map.of("orderId", orderId, "items", reserved, "status", "RELEASED"));
        return new StockReservationResponse(orderId, "RELEASED", reserved);
    }

    private void saveReservation(StockReservationRequest request) {
        try {
            redisTemplate.opsForValue().set(reservationKey(request.orderId()),
                    objectMapper.writeValueAsString(request), RESERVATION_TTL);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist reservation for order: " + request.orderId(), e);
        }
    }

    private StockReservationRequest loadReservation(UUID orderId) {
        String json = redisTemplate.opsForValue().get(reservationKey(orderId));
        if (json == null) {
            throw new NotFoundException("Reservation not found for order: " + orderId);
        }
        try {
            return objectMapper.readValue(json, StockReservationRequest.class);
        } catch (Exception e) {
            throw new BadRequestException("Invalid stored reservation for order: " + orderId);
        }
    }

    private void publishFailure(UUID orderId, String reason) {
        publish(Topics.INVENTORY_RESERVATION_FAILED, orderId,
                Map.of("orderId", orderId, "status", "FAILED", "reason", reason));
    }

    private void publish(String topic, UUID orderId, Map<String, Object> payload) {
        try {
            eventPublisher.publish(topic, orderId.toString(), payload);
        } catch (Exception e) {
            // Non-blocking event publish failure
        }
    }

    private void evictStockCache(UUID productId, UUID pharmacyId) {
        redisTemplate.delete(STOCK_CACHE_KEY_PREFIX + productId + ":" + pharmacyId);
    }

    private String reservationKey(UUID orderId) {
        return RESERVATION_KEY_PREFIX + orderId;
    }
}
