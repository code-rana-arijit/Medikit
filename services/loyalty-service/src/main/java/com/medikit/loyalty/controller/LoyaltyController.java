package com.medikit.loyalty.controller;

import com.medikit.common.security.UserContext;
import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.PageResult;
import com.medikit.loyalty.dto.LoyaltyBalanceResponse;
import com.medikit.loyalty.dto.PointsTransactionDto;
import com.medikit.loyalty.dto.RedeemRequest;
import com.medikit.loyalty.dto.RedeemResponse;
import com.medikit.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/balance")
    public ResponseEntity<LoyaltyBalanceResponse> balance() {
        return ResponseEntity.ok(loyaltyService.getBalance(currentUserId()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PageResult<PointsTransactionDto>> transactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PointsTransactionDto> result =
                loyaltyService.getTransactions(currentUserId(), page, Math.min(size, 50));
        return ResponseEntity.ok(PageResult.from(result));
    }

    @PostMapping("/redeem")
    public ResponseEntity<RedeemResponse> redeem(@Valid @RequestBody RedeemRequest request) {
        return ResponseEntity.ok(loyaltyService.redeem(currentUserId(), request.points()));
    }

    private UUID currentUserId() {
        String userId = UserContext.currentUserId();
        if (userId == null || userId.isBlank()) {
            throw new BadRequestException("Missing X-User-Id header");
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid X-User-Id header");
        }
    }
}
