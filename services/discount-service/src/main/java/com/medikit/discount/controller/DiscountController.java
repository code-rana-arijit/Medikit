package com.medikit.discount.controller;

import com.medikit.common.security.UserContext;
import com.medikit.common.web.PageResult;
import com.medikit.discount.dto.DiscountCodeResponse;
import com.medikit.discount.dto.IssueDiscountRequest;
import com.medikit.discount.dto.RedeemDiscountRequest;
import com.medikit.discount.dto.ValidateDiscountRequest;
import com.medikit.discount.service.DiscountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/discounts")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping("/issue")
    public ResponseEntity<DiscountCodeResponse> issue(@Valid @RequestBody IssueDiscountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                discountService.issue(request.userId(), request.discountAmount(), request.validForDays()));
    }

    @PostMapping("/validate")
    public ResponseEntity<DiscountCodeResponse> validate(@Valid @RequestBody ValidateDiscountRequest request) {
        return ResponseEntity.ok(discountService.validate(request.code(), request.userId()));
    }

    @PostMapping("/redeem")
    public ResponseEntity<DiscountCodeResponse> redeem(@Valid @RequestBody RedeemDiscountRequest request) {
        return ResponseEntity.ok(discountService.redeem(request.code(), request.userId(), request.orderId()));
    }

    @GetMapping("/my")
    public ResponseEntity<PageResult<DiscountCodeResponse>> myCodes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = UUID.fromString(UserContext.currentUserId());
        Page<DiscountCodeResponse> result = discountService.getMyCodes(userId, page, Math.min(size, 50));
        return ResponseEntity.ok(PageResult.from(result));
    }
}
