package com.medikit.delivery.service;

import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.delivery.dto.SlotRequest;
import com.medikit.delivery.entity.DeliverySlot;
import com.medikit.delivery.repository.DeliverySlotRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SlotService {

    private final DeliverySlotRepository slotRepository;

    public SlotService(DeliverySlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    public List<DeliverySlot> getAvailableSlots(UUID pharmacyId, Instant from, Instant to) {
        return slotRepository
                .findByPharmacyIdAndStartTimeAfterAndActiveTrueOrderByStartTimeAsc(pharmacyId, from, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(slot -> !slot.getEndTime().isAfter(to))
                .filter(slot -> slot.getCapacity() - slot.getBooked() > 0)
                .toList();
    }

    @Transactional
    public DeliverySlot createSlot(SlotRequest request) {
        DeliverySlot slot = DeliverySlot.builder()
                .pharmacyId(request.pharmacyId())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .capacity(request.capacity())
                .build();
        return slotRepository.save(slot);
    }

    @Transactional
    public DeliverySlot bookSlot(UUID slotId) {
        DeliverySlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new NotFoundException("Delivery slot not found"));
        if (slot.getBooked() >= slot.getCapacity()) {
            throw new ConflictException("Delivery slot is full");
        }
        slot.setBooked(slot.getBooked() + 1);
        return slotRepository.save(slot);
    }
}
