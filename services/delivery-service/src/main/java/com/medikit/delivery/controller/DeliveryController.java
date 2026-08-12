package com.medikit.delivery.controller;

import com.medikit.common.security.UserContext;
import com.medikit.common.web.BadRequestException;
import com.medikit.delivery.dto.DeliveryResponse;
import com.medikit.delivery.dto.DeliveryStatusRequest;
import com.medikit.delivery.dto.SlotRequest;
import com.medikit.delivery.dto.SlotResponse;
import com.medikit.delivery.entity.DeliveryStatus;
import com.medikit.delivery.service.DeliveryService;
import com.medikit.delivery.service.SlotService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/delivery")
public class DeliveryController {

    private final SlotService slotService;
    private final DeliveryService deliveryService;

    public DeliveryController(SlotService slotService, DeliveryService deliveryService) {
        this.slotService = slotService;
        this.deliveryService = deliveryService;
    }

    @GetMapping("/slots")
    public ResponseEntity<List<SlotResponse>> getSlots(
            @RequestParam UUID pharmacyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        List<SlotResponse> slots = slotService.getAvailableSlots(pharmacyId, from, to)
                .stream()
                .map(slot -> new SlotResponse(
                        slot.getId(),
                        slot.getPharmacyId(),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        slot.getCapacity(),
                        slot.getBooked(),
                        slot.isActive()))
                .toList();
        return ResponseEntity.ok(slots);
    }

    @PostMapping("/slots")
    public ResponseEntity<SlotResponse> createSlot(@Valid @RequestBody SlotRequest request) {
        var slot = slotService.createSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SlotResponse(
                        slot.getId(),
                        slot.getPharmacyId(),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        slot.getCapacity(),
                        slot.getBooked(),
                        slot.isActive()));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<DeliveryResponse> track(@PathVariable UUID orderId) {
        return ResponseEntity.ok(deliveryService.track(orderId));
    }

    @PostMapping("/{orderId}/claim")
    public ResponseEntity<DeliveryResponse> claim(@PathVariable UUID orderId) {
        UUID partnerId = UUID.fromString(UserContext.currentUserId());
        return ResponseEntity.ok(deliveryService.claim(orderId, partnerId));
    }

    @GetMapping("/partner")
    public ResponseEntity<List<DeliveryResponse>> myDeliveries(
            @RequestParam(required = false) String status) {
        UUID partnerId = UUID.fromString(UserContext.currentUserId());
        DeliveryStatus target = null;
        if (status != null && !status.isBlank()) {
            try {
                target = DeliveryStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid delivery status");
            }
        }
        return ResponseEntity.ok(deliveryService.myDeliveries(partnerId, target));
    }

    @GetMapping("/available")
    public ResponseEntity<List<DeliveryResponse>> available() {
        return ResponseEntity.ok(deliveryService.availableDeliveries());
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<DeliveryResponse> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody DeliveryStatusRequest request) {
        UUID callerId = UUID.fromString(UserContext.currentUserId());
        return ResponseEntity.ok(deliveryService.updateStatus(
                orderId,
                DeliveryStatus.valueOf(request.status()),
                request.coordinates(),
                callerId));
    }
}
