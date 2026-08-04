package com.medikit.delivery.dto;

import java.time.Instant;
import java.util.UUID;

public record SlotResponse(
        UUID id,
        UUID pharmacyId,
        Instant startTime,
        Instant endTime,
        int capacity,
        int booked,
        boolean active
) {
}
