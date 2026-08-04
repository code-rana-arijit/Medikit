package com.medikit.product.controller;

import com.medikit.common.web.PageResult;
import com.medikit.product.dto.CategoryRequest;
import com.medikit.product.dto.CategoryResponse;
import com.medikit.product.dto.ProductRequest;
import com.medikit.product.dto.ProductResponse;
import com.medikit.product.service.CategoryService;
import com.medikit.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/products")
    public ResponseEntity<PageResult<ProductResponse>> listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        Page<ProductResponse> result = productService.list(page, Math.min(size, 50), sortBy, direction);
        return ResponseEntity.ok(PageResult.from(result));
    }

    @GetMapping("/products/search")
    public ResponseEntity<PageResult<ProductResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ProductResponse> result = productService.search(q, categoryId, minPrice, maxPrice, type, page, Math.min(size, 50));
        return ResponseEntity.ok(PageResult.from(result));
    }

    @GetMapping("/products/trending")
    public ResponseEntity<PageResult<ProductResponse>> trending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<ProductResponse> result = productService.trending(page, Math.min(size, 50));
        return ResponseEntity.ok(PageResult.from(result));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.get(id));
    }

    @GetMapping("/products/{id}/alternatives")
    public ResponseEntity<List<ProductResponse>> alternatives(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.alternatives(id));
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<Map<String, String>> deactivateProduct(@PathVariable UUID id) {
        productService.deactivate(id);
        return ResponseEntity.ok(Map.of("message", "Product deactivated"));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryResponse>> categories() {
        return ResponseEntity.ok(categoryService.getAll());
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Category deactivated"));
    }
}
