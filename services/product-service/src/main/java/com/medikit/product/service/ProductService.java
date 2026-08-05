package com.medikit.product.service;

import com.medikit.common.event.EventPublisher;
import com.medikit.common.event.Topics;
import com.medikit.common.web.NotFoundException;
import com.medikit.product.dto.ProductRequest;
import com.medikit.product.dto.ProductResponse;
import com.medikit.product.entity.Category;
import com.medikit.product.entity.Product;
import com.medikit.product.repository.CategoryRepository;
import com.medikit.product.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final EventPublisher eventPublisher;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          EventPublisher eventPublisher) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.eventPublisher = eventPublisher;
    }

    public Page<ProductResponse> list(int page, int size, String sortBy, String direction) {
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        return productRepository.findAll(PageRequest.of(page, size, sort)).map(ProductResponse::from);
    }

    public Page<ProductResponse> search(String q, UUID categoryId, BigDecimal minPrice,
                                        BigDecimal maxPrice, String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Product.ProductType productType = type != null ? Product.ProductType.valueOf(type) : null;
        return productRepository.search(q, categoryId, minPrice, maxPrice, productType, pageable)
                .map(ProductResponse::from);
    }

    public Page<ProductResponse> byCategory(UUID categoryId, int page, int size) {
        return productRepository.findByActiveTrueAndCategoryId(categoryId, PageRequest.of(page, size))
                .map(ProductResponse::from);
    }

    public Page<ProductResponse> byPharmacy(UUID pharmacyId, int page, int size) {
        return productRepository.findByActiveTrueAndPharmacyId(pharmacyId, PageRequest.of(page, size))
                .map(ProductResponse::from);
    }

    public Page<ProductResponse> trending(int page, int size) {
        return productRepository.findTrending(PageRequest.of(page, size)).map(ProductResponse::from);
    }

    public List<ProductResponse> alternatives(UUID productId) {
        Product product = getEntity(productId);
        if (product.getSaltComposition() == null || product.getSaltComposition().isBlank()) {
            return List.of();
        }
        return productRepository.findAlternatives(product.getSaltComposition(), PageRequest.of(0, 10))
                .stream()
                .filter(p -> !p.getId().equals(productId))
                .map(ProductResponse::from)
                .toList();
    }

    @Cacheable(cacheNames = "product", key = "#id", unless = "#result == null")
    public ProductResponse get(UUID id) {
        return ProductResponse.from(getEntity(id));
    }

    @Transactional
    @CacheEvict(cacheNames = "product", key = "#result.id()")
    public ProductResponse create(ProductRequest request) {
        Category category = request.categoryId() != null
                ? categoryRepository.findById(request.categoryId())
                        .orElseThrow(() -> new NotFoundException("Category not found"))
                : null;

        Product product = Product.builder()
                .name(request.name())
                .description(request.description())
                .saltComposition(request.saltComposition())
                .manufacturer(request.manufacturer())
                .mrp(request.mrp())
                .sellingPrice(request.sellingPrice())
                .prescriptionRequired(request.prescriptionRequired())
                .productType(Product.ProductType.valueOf(request.productType()))
                .packaging(request.packaging())
                .packSize(request.packSize())
                .category(category)
                .pharmacyId(request.pharmacyId())
                .imageUrl(request.imageUrl())
                .build();

        Product saved = productRepository.save(product);
        publishUpdate(saved);
        return ProductResponse.from(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "product", key = "#id")
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = getEntity(id);
        Category category = request.categoryId() != null
                ? categoryRepository.findById(request.categoryId())
                        .orElseThrow(() -> new NotFoundException("Category not found"))
                : null;

        product.setName(request.name());
        product.setDescription(request.description());
        product.setSaltComposition(request.saltComposition());
        product.setManufacturer(request.manufacturer());
        product.setMrp(request.mrp());
        product.setSellingPrice(request.sellingPrice());
        product.setPrescriptionRequired(request.prescriptionRequired());
        product.setProductType(Product.ProductType.valueOf(request.productType()));
        product.setPackaging(request.packaging());
        product.setPackSize(request.packSize());
        product.setCategory(category);
        product.setPharmacyId(request.pharmacyId());
        product.setImageUrl(request.imageUrl());

        Product saved = productRepository.save(product);
        publishUpdate(saved);
        return ProductResponse.from(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = "product", key = "#id")
    public void deactivate(UUID id) {
        Product product = getEntity(id);
        product.setActive(false);
        productRepository.save(product);
        publishUpdate(product);
    }

    public Map<UUID, Product> findByIds(List<UUID> ids) {
        return productRepository.findAllById(ids).stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
    }

    private Product getEntity(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    private void publishUpdate(Product product) {
        try {
            eventPublisher.publish(Topics.PRODUCT_UPDATED, product.getId().toString(),
                    Map.of(
                            "productId", product.getId(),
                            "name", product.getName(),
                            "sellingPrice", product.getSellingPrice(),
                            "prescriptionRequired", product.isPrescriptionRequired(),
                            "pharmacyId", product.getPharmacyId(),
                            "active", product.isActive()
                    ));
        } catch (Exception e) {
            // Non-blocking event publish failure
        }
    }
}
