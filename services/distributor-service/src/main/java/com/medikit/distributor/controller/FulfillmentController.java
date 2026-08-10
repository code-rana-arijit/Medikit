package com.medikit.distributor.controller;

import com.medikit.common.security.UserContext;
import com.medikit.distributor.dto.FulfillmentClaimRequest;
import com.medikit.distributor.dto.FulfillmentResponse;
import com.medikit.distributor.dto.FulfillmentStatusRequest;
import com.medikit.distributor.entity.FulfillmentStatus;
import com.medikit.distributor.service.FulfillmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fulfillments")
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    public FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping("/claim")
    public ResponseEntity<FulfillmentResponse> claim(@Valid @RequestBody FulfillmentClaimRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fulfillmentService.claim(currentUserId(), request.orderId()));
    }

    @PatchMapping("/{fulfillmentId}/status")
    public ResponseEntity<FulfillmentResponse> updateStatus(@PathVariable UUID fulfillmentId,
                                                            @Valid @RequestBody FulfillmentStatusRequest request) {
        FulfillmentStatus target;
        try {
            target = FulfillmentStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.medikit.common.web.BadRequestException("Invalid fulfillment status");
        }
        return ResponseEntity.ok(fulfillmentService.updateStatus(currentUserId(), fulfillmentId, target));
    }

    @GetMapping("/{fulfillmentId}")
    public ResponseEntity<FulfillmentResponse> get(@PathVariable UUID fulfillmentId) {
        return ResponseEntity.ok(fulfillmentService.getFulfillment(currentUserId(), fulfillmentId));
    }

    @GetMapping
    public ResponseEntity<List<FulfillmentResponse>> myFulfillments() {
        return ResponseEntity.ok(fulfillmentService.myFulfillments(currentUserId()));
    }

    private UUID currentUserId() {
        return UUID.fromString(UserContext.currentUserId());
    }
}
