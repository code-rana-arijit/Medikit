package com.medikit.user.controller;

import com.medikit.common.security.UserContext;
import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.PageResult;
import com.medikit.user.dto.PharmacistReviewRequest;
import com.medikit.user.dto.PharmacistVerificationRequest;
import com.medikit.user.dto.PharmacistVerificationResponse;
import com.medikit.user.service.PharmacistVerificationService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/v1")
public class PharmacistVerificationController {

    private final PharmacistVerificationService verificationService;

    public PharmacistVerificationController(PharmacistVerificationService verificationService) {
        this.verificationService = verificationService;
    }

    @PostMapping("/pharmacists/verification")
    public ResponseEntity<PharmacistVerificationResponse> submit(
            @Valid @RequestBody PharmacistVerificationRequest request) {
        UUID userId = UUID.fromString(UserContext.currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(verificationService.submit(userId, request));
    }

    @GetMapping("/pharmacists/verification/me")
    public ResponseEntity<PharmacistVerificationResponse> myVerification() {
        UUID userId = UUID.fromString(UserContext.currentUserId());
        return ResponseEntity.ok(verificationService.getByUser(userId));
    }

    @GetMapping("/pharmacists/verification/{userId}")
    public ResponseEntity<PharmacistVerificationResponse> getByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(verificationService.getByUser(userId));
    }

    @GetMapping("/admin/pharmacists/verifications")
    public ResponseEntity<PageResult<PharmacistVerificationResponse>> list(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PharmacistVerificationResponse> result =
                verificationService.listByStatus(status, page, size);
        return ResponseEntity.ok(PageResult.from(result));
    }

    @PostMapping("/admin/pharmacists/verifications/review")
    public ResponseEntity<PharmacistVerificationResponse> review(
            @Valid @RequestBody PharmacistReviewRequest request) {
        String reviewer = UserContext.currentUserEmail();
        return ResponseEntity.ok(verificationService.review(request, reviewer));
    }
}
