package com.medikit.delivery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;
import java.util.UUID;

public record SlotRequest(
        @NotNull UUID pharmacyId,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @Positive int capacity
) {
}
