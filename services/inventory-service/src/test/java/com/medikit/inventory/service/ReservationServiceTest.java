package com.medikit.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medikit.common.event.EventPublisher;
import com.medikit.common.event.Topics;
import com.medikit.inventory.dto.StockReservationRequest;
import com.medikit.inventory.dto.StockReservationResponse;
import com.medikit.inventory.entity.InventoryItem;
import com.medikit.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private EventPublisher eventPublisher;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ReservationService reservationService;

    private UUID orderId;
    private UUID pharmacyId;
    private UUID productId;
    private InventoryItem item;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        pharmacyId = UUID.randomUUID();
        productId = UUID.randomUUID();
        item = InventoryItem.builder()
                .id(UUID.randomUUID())
                .productId(productId)
                .pharmacyId(pharmacyId)
                .quantityAvailable(10)
                .reservedQuantity(2)
                .minStockLevel(3)
                .maxStockLevel(50)
                .active(true)
                .build();
    }

    @Test
    void reserveStock_reservesQuantityWhenStockAvailable() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(inventoryRepository.findByProductIdInAndPharmacyIdForUpdate(List.of(productId), pharmacyId))
                .thenReturn(List.of(item));

        StockReservationResponse response = reservationService.reserveStock(
                new StockReservationRequest(orderId, pharmacyId,
                        List.of(new StockReservationRequest.Item(productId, 4))));

        assertThat(response.status()).isEqualTo("RESERVED");
        assertThat(response.items()).hasSize(1);
        assertThat(item.getQuantityAvailable()).isEqualTo(6);
        assertThat(item.getReservedQuantity()).isEqualTo(6);
        verify(valueOperations).set(eq("medikit:inventory:reservation:" + orderId), anyString(),
                eq(Duration.ofMinutes(30)));
        verify(eventPublisher).publish(eq(Topics.INVENTORY_RESERVED), eq(orderId.toString()), anyMap());
    }

    @Test
    void reserveStock_returnsFailedWhenInsufficientStock() {
        when(inventoryRepository.findByProductIdInAndPharmacyIdForUpdate(List.of(productId), pharmacyId))
                .thenReturn(List.of(item));

        StockReservationResponse response = reservationService.reserveStock(
                new StockReservationRequest(orderId, pharmacyId,
                        List.of(new StockReservationRequest.Item(productId, 99))));

        assertThat(response.status()).isEqualTo("FAILED");
        assertThat(response.items()).isEmpty();
        assertThat(item.getQuantityAvailable()).isEqualTo(10);
        assertThat(item.getReservedQuantity()).isEqualTo(2);
        verify(valueOperations, never()).set(eq("medikit:inventory:reservation:" + orderId), anyString(),
                any(Duration.class));
        verify(eventPublisher).publish(eq(Topics.INVENTORY_RESERVATION_FAILED), eq(orderId.toString()), anyMap());
    }
}
