package com.medikit.product.controller;

import com.medikit.common.web.PageResult;
import com.medikit.product.dto.PharmacyResponse;
import com.medikit.product.service.PharmacyService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pharmacies")
public class PharmacyController {

    private final PharmacyService pharmacyService;

    public PharmacyController(PharmacyService pharmacyService) {
        this.pharmacyService = pharmacyService;
    }

    @GetMapping
    public ResponseEntity<PageResult<PharmacyResponse>> list(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        Page<PharmacyResponse> result = pharmacyService.list(page, size);
        return ResponseEntity.ok(PageResult.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PharmacyResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(pharmacyService.get(id));
    }

    @PostMapping
    public ResponseEntity<PharmacyResponse> register(
            @RequestBody PharmacyService.PharmacyRegistration request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pharmacyService.create(request));
    }

    @PostMapping("/{id}/revalidate")
    public ResponseEntity<PharmacyResponse> revalidate(@PathVariable UUID id) {
        return ResponseEntity.ok(pharmacyService.revalidate(id));
    }
}
