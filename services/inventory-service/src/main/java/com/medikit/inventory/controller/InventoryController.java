package com.medikit.inventory.controller;

import com.medikit.inventory.dto.StockBulkRequest;
import com.medikit.inventory.dto.StockLevelResponse;
import com.medikit.inventory.dto.StockReservationRequest;
import com.medikit.inventory.dto.StockReservationResponse;
import com.medikit.inventory.dto.StockUpdateRequest;
import com.medikit.inventory.service.InventoryService;
import com.medikit.inventory.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ReservationService reservationService;

    public InventoryController(InventoryService inventoryService, ReservationService reservationService) {
        this.inventoryService = inventoryService;
        this.reservationService = reservationService;
    }

    @GetMapping("/stock")
    public ResponseEntity<StockLevelResponse> getStock(@RequestParam UUID productId,
                                                       @RequestParam UUID pharmacyId) {
        return ResponseEntity.ok(inventoryService.getStock(productId, pharmacyId));
    }

    @PostMapping("/stock/bulk")
    public ResponseEntity<List<StockLevelResponse>> bulkStock(@Valid @RequestBody StockBulkRequest request) {
        return ResponseEntity.ok(inventoryService.getStockBulk(request.productIds(), request.pharmacyId()));
    }

    @PutMapping("/stock")
    public ResponseEntity<StockLevelResponse> updateStock(@Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(inventoryService.updateStock(request));
    }

    @PostMapping("/reserve")
    public ResponseEntity<StockReservationResponse> reserve(@Valid @RequestBody StockReservationRequest request) {
        return ResponseEntity.ok(reservationService.reserveStock(request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<StockReservationResponse> confirm(@RequestParam UUID orderId) {
        return ResponseEntity.ok(reservationService.confirmReservation(orderId));
    }

    @PostMapping("/release")
    public ResponseEntity<StockReservationResponse> release(@RequestParam UUID orderId) {
        return ResponseEntity.ok(reservationService.releaseReservation(orderId));
    }
}
