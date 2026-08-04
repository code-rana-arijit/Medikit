package com.medikit.product.dto;

import com.medikit.product.entity.Product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String description,
        String saltComposition,
        String manufacturer,
        BigDecimal mrp,
        BigDecimal sellingPrice,
        boolean prescriptionRequired,
        String productType,
        String packaging,
        String packSize,
        UUID categoryId,
        String categoryName,
        UUID pharmacyId,
        String imageUrl,
        int rating,
        long ratingCount,
        double discountPercent
) {
    public static ProductResponse from(Product p) {
        double discount = p.getMrp() != null && p.getMrp().compareTo(BigDecimal.ZERO) > 0
                ? p.getMrp().subtract(p.getSellingPrice()).divide(p.getMrp(), 4, java.math.RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue()
                : 0;
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getSaltComposition(),
                p.getManufacturer(),
                p.getMrp(),
                p.getSellingPrice(),
                p.isPrescriptionRequired(),
                p.getProductType().name(),
                p.getPackaging(),
                p.getPackSize(),
                p.getCategory() != null ? p.getCategory().getId() : null,
                p.getCategory() != null ? p.getCategory().getName() : null,
                p.getPharmacyId(),
                p.getImageUrl(),
                p.getRating(),
                p.getRatingCount(),
                Math.round(discount * 100.0) / 100.0
        );
    }
}
