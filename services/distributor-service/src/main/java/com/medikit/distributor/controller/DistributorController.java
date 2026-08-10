package com.medikit.distributor.controller;

import com.medikit.common.security.UserContext;
import com.medikit.distributor.dto.CatalogItemRequest;
import com.medikit.distributor.dto.CatalogItemResponse;
import com.medikit.distributor.dto.DistributorRegisterRequest;
import com.medikit.distributor.dto.DistributorResponse;
import com.medikit.distributor.service.DistributorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/distributors")
public class DistributorController {

    private final DistributorService distributorService;

    public DistributorController(DistributorService distributorService) {
        this.distributorService = distributorService;
    }

    @PostMapping("/register")
    public ResponseEntity<DistributorResponse> register(@Valid @RequestBody DistributorRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(distributorService.register(currentUserId(), request));
    }

    @PutMapping("/me")
    public ResponseEntity<DistributorResponse> updateProfile(@Valid @RequestBody DistributorRegisterRequest request) {
        return ResponseEntity.ok(distributorService.updateProfile(currentUserId(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<DistributorResponse> me() {
        return ResponseEntity.ok(distributorService.getMine(currentUserId()));
    }

    @GetMapping("/{distributorId}")
    public ResponseEntity<DistributorResponse> getById(@PathVariable UUID distributorId) {
        return ResponseEntity.ok(distributorService.getById(distributorId));
    }

    @GetMapping
    public ResponseEntity<List<DistributorResponse>> list() {
        return ResponseEntity.ok(distributorService.listActive());
    }

    @GetMapping("/{distributorId}/catalog")
    public ResponseEntity<List<CatalogItemResponse>> catalog(@PathVariable UUID distributorId) {
        return ResponseEntity.ok(distributorService.getCatalog(distributorId));
    }

    @PostMapping("/me/catalog")
    public ResponseEntity<CatalogItemResponse> addCatalogItem(@Valid @RequestBody CatalogItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(distributorService.addCatalogItem(currentUserId(), request));
    }

    @PutMapping("/me/catalog/{itemId}")
    public ResponseEntity<CatalogItemResponse> updateCatalogItem(@PathVariable UUID itemId,
                                                                 @Valid @RequestBody CatalogItemRequest request) {
        return ResponseEntity.ok(distributorService.updateCatalogItem(currentUserId(), itemId, request));
    }

    @DeleteMapping("/me/catalog/{itemId}")
    public ResponseEntity<Map<String, String>> deleteCatalogItem(@PathVariable UUID itemId) {
        distributorService.deleteCatalogItem(currentUserId(), itemId);
        return ResponseEntity.ok(Map.of("message", "Catalog item deleted"));
    }

    private UUID currentUserId() {
        return UUID.fromString(UserContext.currentUserId());
    }
}
