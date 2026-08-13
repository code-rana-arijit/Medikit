package com.medikit.delivery.service;

import com.medikit.common.event.EventPublisher;
import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.delivery.dto.DeliveryStatusRequest;
import com.medikit.delivery.entity.Delivery;
import com.medikit.delivery.entity.DeliveryStatus;
import com.medikit.delivery.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private EventPublisher eventPublisher;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private DeliveryService deliveryService;

    private UUID orderId;
    private UUID partnerId;
    private Delivery delivery;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        partnerId = UUID.randomUUID();
        delivery = Delivery.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .userId(UUID.randomUUID())
                .pharmacyId(UUID.randomUUID())
                .slotId(UUID.randomUUID())
                .status(DeliveryStatus.PENDING)
                .build();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void claim_assignsPartnerAndMarksAssigned() {
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(delivery)).thenReturn(delivery);

        var response = deliveryService.claim(orderId, partnerId);

        assertThat(response.status()).isEqualTo("ASSIGNED");
        assertThat(response.partnerId()).isEqualTo(partnerId);
        verify(deliveryRepository).save(delivery);
    }

    @Test
    void claim_throwsConflictWhenAlreadyAssigned() {
        delivery.setPartnerId(partnerId);
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.claim(orderId, UUID.randomUUID()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Delivery already assigned to a partner");
    }

    @Test
    void updateStatus_throwsBadRequestWhenNoPartner() {
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.updateStatus(
                orderId, DeliveryStatus.PICKED_UP, null, partnerId))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Delivery has no assigned partner yet");
    }

    @Test
    void updateStatus_setsDeliveredAtOnDelivered() {
        delivery.setPartnerId(partnerId);
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(delivery)).thenReturn(delivery);

        var response = deliveryService.updateStatus(
                orderId, DeliveryStatus.DELIVERED,
                new DeliveryStatusRequest.Coordinates(18.52, 73.85), partnerId);

        assertThat(response.status()).isEqualTo("DELIVERED");
        assertThat(response.partnerLatitude()).isEqualTo(18.52);
        assertThat(delivery.getDeliveredAt()).isNotNull();
    }

    @Test
    void updateStatus_throwsForbiddenForWrongPartner() {
        delivery.setPartnerId(partnerId);
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.updateStatus(
                orderId, DeliveryStatus.PICKED_UP, null, UUID.randomUUID()))
                .isInstanceOf(com.medikit.common.web.ForbiddenException.class)
                .hasMessage("Only the assigned delivery partner can update this delivery");
    }

    @Test
    void track_throwsNotFoundWhenMissing() {
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryService.track(orderId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void myDeliveries_returnsOnlyMatchingPartner() {
        UUID otherPartner = UUID.randomUUID();
        Delivery mine = Delivery.builder()
                .id(UUID.randomUUID()).orderId(orderId)
                .userId(UUID.randomUUID()).pharmacyId(UUID.randomUUID())
                .slotId(UUID.randomUUID()).status(DeliveryStatus.ASSIGNED)
                .partnerId(partnerId).build();
        Delivery theirs = Delivery.builder()
                .id(UUID.randomUUID()).orderId(UUID.randomUUID())
                .userId(UUID.randomUUID()).pharmacyId(UUID.randomUUID())
                .slotId(UUID.randomUUID()).status(DeliveryStatus.ASSIGNED)
                .partnerId(otherPartner).build();
        when(deliveryRepository.findByPartnerId(partnerId)).thenReturn(List.of(mine, theirs));

        var list = deliveryService.myDeliveries(partnerId, null);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).partnerId()).isEqualTo(partnerId);
    }

    @Test
    void availableDeliveries_returnsPendingOnly() {
        when(deliveryRepository.findByPartnerIdIsNullAndStatus(DeliveryStatus.PENDING))
                .thenReturn(List.of(delivery));

        var list = deliveryService.availableDeliveries();

        assertThat(list).hasSize(1);
        assertThat(list.get(0).status()).isEqualTo("PENDING");
    }

    @Test
    void claim_throwsConflictWhenCancelled() {
        delivery.setStatus(DeliveryStatus.CANCELLED);
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.claim(orderId, partnerId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot claim cancelled delivery");
    }

    @Test
    void updateLocation_savesPartnerCoordinates() {
        delivery.setPartnerId(partnerId);
        delivery.setStatus(DeliveryStatus.IN_TRANSIT);
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(delivery)).thenReturn(delivery);

        var response = deliveryService.updateLocation(orderId, 12.9716, 77.5946, partnerId);

        assertThat(response.partnerLatitude()).isEqualTo(12.9716);
        assertThat(response.partnerLongitude()).isEqualTo(77.5946);
    }

    @Test
    void updateLocation_throwsForbiddenForWrongPartner() {
        delivery.setPartnerId(partnerId);
        delivery.setStatus(DeliveryStatus.IN_TRANSIT);
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.updateLocation(orderId, 12.9716, 77.5946, UUID.randomUUID()))
                .isInstanceOf(com.medikit.common.web.ForbiddenException.class);
    }

    @Test
    void updateLocation_throwsConflictWhenDelivered() {
        delivery.setPartnerId(partnerId);
        delivery.setStatus(DeliveryStatus.DELIVERED);
        when(deliveryRepository.findByOrderId(orderId)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> deliveryService.updateLocation(orderId, 12.9716, 77.5946, partnerId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot update location for a delivered delivery");
    }

    @Test
    void adminDeliveries_filtersByStatus() {
        Delivery assigned = Delivery.builder()
                .id(UUID.randomUUID()).orderId(orderId)
                .userId(UUID.randomUUID()).pharmacyId(UUID.randomUUID())
                .slotId(UUID.randomUUID()).status(DeliveryStatus.ASSIGNED)
                .partnerId(partnerId).build();
        org.springframework.data.domain.Page<Delivery> page =
                new org.springframework.data.domain.PageImpl<>(List.of(assigned));
        when(deliveryRepository.findByStatus(DeliveryStatus.ASSIGNED,
                org.springframework.data.domain.PageRequest.of(0, 20))).thenReturn(page);

        var result = deliveryService.adminDeliveries(DeliveryStatus.ASSIGNED, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo("ASSIGNED");
    }

    @Test
    void adminStats_countsByStatus() {
        when(deliveryRepository.countByStatus(DeliveryStatus.PENDING)).thenReturn(3L);
        when(deliveryRepository.countByStatus(DeliveryStatus.ASSIGNED)).thenReturn(2L);
        when(deliveryRepository.countByStatus(DeliveryStatus.PICKED_UP)).thenReturn(0L);
        when(deliveryRepository.countByStatus(DeliveryStatus.IN_TRANSIT)).thenReturn(1L);
        when(deliveryRepository.countByStatus(DeliveryStatus.DELIVERED)).thenReturn(10L);
        when(deliveryRepository.countByStatus(DeliveryStatus.CANCELLED)).thenReturn(1L);

        var stats = deliveryService.adminStats();

        assertThat(stats).containsEntry("PENDING", 3L)
                .containsEntry("ASSIGNED", 2L)
                .containsEntry("DELIVERED", 10L);
    }
}
