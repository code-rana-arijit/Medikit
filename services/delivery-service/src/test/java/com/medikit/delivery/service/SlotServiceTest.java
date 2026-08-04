package com.medikit.delivery.service;

import com.medikit.common.web.ConflictException;
import com.medikit.delivery.entity.DeliverySlot;
import com.medikit.delivery.repository.DeliverySlotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotServiceTest {

    @Mock
    private DeliverySlotRepository slotRepository;

    @InjectMocks
    private SlotService slotService;

    @Test
    void bookSlot_throwsConflictWhenCapacityExhausted() {
        DeliverySlot slot = DeliverySlot.builder()
                .id(UUID.randomUUID())
                .pharmacyId(UUID.randomUUID())
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(3600))
                .capacity(2)
                .booked(2)
                .build();
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> slotService.bookSlot(slot.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Delivery slot is full");
    }

    @Test
    void bookSlot_incrementsBookedWhenCapacityAvailable() {
        DeliverySlot slot = DeliverySlot.builder()
                .id(UUID.randomUUID())
                .pharmacyId(UUID.randomUUID())
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(3600))
                .capacity(5)
                .booked(2)
                .build();
        when(slotRepository.findByIdForUpdate(slot.getId())).thenReturn(Optional.of(slot));

        slotService.bookSlot(slot.getId());

        assertThat(slot.getBooked()).isEqualTo(3);
        verify(slotRepository).save(slot);
    }
}
