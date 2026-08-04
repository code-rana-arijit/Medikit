package com.medikit.search.dto;

import java.math.BigDecimal;

public record ProductUpdatedEvent(
        String productId,
        String name,
        BigDecimal sellingPrice,
        BigDecimal mrp,
        String pharmacyId,
        boolean prescriptionRequired,
        boolean active
) {
}
