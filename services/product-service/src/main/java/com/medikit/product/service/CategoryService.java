package com.medikit.product.service;

import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.product.dto.CategoryRequest;
import com.medikit.product.dto.CategoryResponse;
import com.medikit.product.entity.Category;
import com.medikit.product.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private static final String CACHE_KEY = "medikit:categories";

    private final CategoryRepository categoryRepository;
    private final StringRedisTemplate redisTemplate;
    private final Duration cacheTtl;

    public CategoryService(CategoryRepository categoryRepository,
                           StringRedisTemplate redisTemplate,
                           @Value("${medikit.cache.category-ttl-seconds:7200}") long cacheTtl) {
        this.categoryRepository = categoryRepository;
        this.redisTemplate = redisTemplate;
        this.cacheTtl = Duration.ofSeconds(cacheTtl);
    }

    public List<CategoryResponse> getAll() {
        String cached = redisTemplate.opsForValue().get(CACHE_KEY);
        if (cached != null) {
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(cached,
                        mapper.getTypeFactory().constructCollectionType(List.class, CategoryResponse.class));
            } catch (Exception ignored) {
            }
        }
        List<CategoryResponse> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toResponse)
                .toList();
        try {
            redisTemplate.opsForValue().set(CACHE_KEY,
                    new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(categories),
                    cacheTtl);
        } catch (Exception ignored) {
        }
        return categories;
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new ConflictException("Category already exists");
        }
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .sortOrder(request.sortOrder())
                .build();
        evict();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        category.setName(request.name());
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder());
        evict();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        category.setActive(false);
        categoryRepository.save(category);
        evict();
    }

    private void evict() {
        redisTemplate.delete(CACHE_KEY);
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.getSortOrder(), c.getCreatedAt());
    }
}
