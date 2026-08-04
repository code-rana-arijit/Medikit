package com.medikit.delivery.dto;

import java.util.UUID;

public record OrderConfirmedEvent(
        UUID orderId,
        UUID userId,
        UUID pharmacyId,
        UUID slotId,
        Double customerLatitude,
        Double customerLongitude
) {
}
