package com.medikit.distributor.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.distributor.dto.CatalogItemRequest;
import com.medikit.distributor.dto.CatalogItemResponse;
import com.medikit.distributor.dto.DistributorRegisterRequest;
import com.medikit.distributor.dto.DistributorResponse;
import com.medikit.distributor.entity.CatalogItem;
import com.medikit.distributor.entity.DistributorProfile;
import com.medikit.distributor.repository.CatalogItemRepository;
import com.medikit.distributor.repository.DistributorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DistributorService {

    private final DistributorProfileRepository profileRepository;
    private final CatalogItemRepository catalogRepository;

    public DistributorService(DistributorProfileRepository profileRepository,
                              CatalogItemRepository catalogRepository) {
        this.profileRepository = profileRepository;
        this.catalogRepository = catalogRepository;
    }

    @Transactional
    public DistributorResponse register(UUID userId, DistributorRegisterRequest request) {
        if (profileRepository.existsByUserId(userId)) {
            throw new ConflictException("Distributor profile already exists for this user");
        }
        DistributorProfile profile = DistributorProfile.builder()
                .userId(userId)
                .shopName(request.shopName())
                .licenseNumber(request.licenseNumber())
                .address(request.address())
                .city(request.city())
                .build();
        return toResponse(profileRepository.save(profile));
    }

    @Transactional
    public DistributorResponse updateProfile(UUID userId, DistributorRegisterRequest request) {
        DistributorProfile profile = findForUser(userId);
        if (request.shopName() != null && !request.shopName().isBlank()) {
            profile.setShopName(request.shopName());
        }
        if (request.licenseNumber() != null && !request.licenseNumber().isBlank()) {
            profile.setLicenseNumber(request.licenseNumber());
        }
        if (request.address() != null && !request.address().isBlank()) {
            profile.setAddress(request.address());
        }
        if (request.city() != null && !request.city().isBlank()) {
            profile.setCity(request.city());
        }
        return toResponse(profileRepository.save(profile));
    }

    public DistributorResponse getMine(UUID userId) {
        return toResponse(findForUser(userId));
    }

    public DistributorResponse getById(UUID distributorId) {
        return toResponse(profileRepository.findById(distributorId)
                .orElseThrow(() -> new NotFoundException("Distributor not found")));
    }

    public List<DistributorResponse> listActive() {
        return profileRepository.findByActiveTrueOrderByCreatedAtAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CatalogItemResponse addCatalogItem(UUID userId, CatalogItemRequest request) {
        DistributorProfile profile = findForUser(userId);
        if (catalogRepository.findByDistributorIdAndProductId(profile.getId(), request.productId()).isPresent()) {
            throw new ConflictException("Product already in catalog");
        }
        CatalogItem item = CatalogItem.builder()
                .distributorId(profile.getId())
                .productId(request.productId())
                .productName(request.productName())
                .unitPrice(request.unitPrice())
                .packSize(request.packSize())
                .stockQty(request.stockQty())
                .build();
        return toCatalogResponse(catalogRepository.save(item));
    }

    @Transactional
    public CatalogItemResponse updateCatalogItem(UUID userId, UUID itemId, CatalogItemRequest request) {
        DistributorProfile profile = findForUser(userId);
        CatalogItem item = catalogRepository.findByDistributorIdAndId(profile.getId(), itemId)
                .orElseThrow(() -> new NotFoundException("Catalog item not found"));
        item.setProductName(request.productName());
        item.setUnitPrice(request.unitPrice());
        item.setPackSize(request.packSize());
        item.setStockQty(request.stockQty());
        return toCatalogResponse(catalogRepository.save(item));
    }

    @Transactional
    public void deleteCatalogItem(UUID userId, UUID itemId) {
        DistributorProfile profile = findForUser(userId);
        CatalogItem item = catalogRepository.findByDistributorIdAndId(profile.getId(), itemId)
                .orElseThrow(() -> new NotFoundException("Catalog item not found"));
        catalogRepository.delete(item);
    }

    @Transactional
    public void adjustStock(UUID distributorId, UUID productId, int delta) {
        CatalogItem item = catalogRepository.findByDistributorIdAndProductId(distributorId, productId)
                .orElseThrow(() -> new NotFoundException("Catalog item not found"));
        int updated = item.getStockQty() + delta;
        if (updated < 0) {
            throw new BadRequestException("Insufficient distributor stock");
        }
        item.setStockQty(updated);
        catalogRepository.save(item);
    }

    public List<CatalogItemResponse> getCatalog(UUID distributorId) {
        return catalogRepository.findByDistributorIdOrderByProductNameAsc(distributorId)
                .stream()
                .map(this::toCatalogResponse)
                .toList();
    }

    public DistributorProfile findForUser(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Distributor profile not found"));
    }

    private DistributorResponse toResponse(DistributorProfile p) {
        return new DistributorResponse(
                p.getId(),
                p.getUserId(),
                p.getShopName(),
                p.getLicenseNumber(),
                p.getAddress(),
                p.getCity(),
                p.isActive(),
                p.getCreatedAt());
    }

    private CatalogItemResponse toCatalogResponse(CatalogItem item) {
        return new CatalogItemResponse(
                item.getId(),
                item.getDistributorId(),
                item.getProductId(),
                item.getProductName(),
                item.getUnitPrice(),
                item.getPackSize(),
                item.getStockQty(),
                item.getCreatedAt());
    }
}
