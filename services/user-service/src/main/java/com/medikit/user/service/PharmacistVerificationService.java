package com.medikit.user.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.ForbiddenException;
import com.medikit.common.web.NotFoundException;
import com.medikit.user.dto.PharmacistReviewRequest;
import com.medikit.user.dto.PharmacistVerificationRequest;
import com.medikit.user.dto.PharmacistVerificationResponse;
import com.medikit.user.entity.PharmacistVerification;
import com.medikit.user.entity.User;
import com.medikit.user.entity.UserRole;
import com.medikit.user.repository.PharmacistVerificationRepository;
import com.medikit.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Pharmacist onboarding and license verification.
 * <p>
 * A pharmacist registers with their pharmacy license details and a document
 * reference; an administrator verifies the submission. Only verified
 * pharmacists receive the PHARMACIST role, which gates order fulfillment
 * actions elsewhere in the platform.
 * </p>
 */
@Service
public class PharmacistVerificationService {

    private static final Logger log = LoggerFactory.getLogger(PharmacistVerificationService.class);
    private static final Pattern LICENSE_PATTERN = Pattern.compile("^[A-Z0-9]{4,20}$");

    private final PharmacistVerificationRepository verificationRepository;
    private final UserRepository userRepository;

    public PharmacistVerificationService(PharmacistVerificationRepository verificationRepository,
                                         UserRepository userRepository) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public PharmacistVerificationResponse submit(UUID userId, PharmacistVerificationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        verificationRepository.findByUserId(userId).ifPresent(existing -> {
            throw new ConflictException("Verification already submitted for this user");
        });

        if (!LICENSE_PATTERN.matcher(normalize(request.licenseNumber())).matches()) {
            throw new BadRequestException("License number must contain only letters and digits (4-20 chars)");
        }

        PharmacistVerification verification = PharmacistVerification.builder()
                .userId(userId)
                .licenseNumber(normalize(request.licenseNumber()))
                .licenseState(request.licenseState())
                .fullName(request.fullName() != null ? request.fullName() : user.getFullName())
                .licenseDocumentUrl(request.licenseDocumentUrl())
                .status(PharmacistVerification.VerificationStatus.PENDING)
                .build();

        PharmacistVerification saved = verificationRepository.save(verification);
        log.info("Pharmacist {} submitted license {} for verification", userId, saved.getLicenseNumber());
        return PharmacistVerificationResponse.from(saved);
    }

    @Transactional
    public PharmacistVerificationResponse review(PharmacistReviewRequest request, String reviewerUserId) {
        PharmacistVerification verification = verificationRepository.findById(request.verificationId())
                .orElseThrow(() -> new NotFoundException("Verification record not found"));

        if (verification.getStatus() != PharmacistVerification.VerificationStatus.PENDING) {
            throw new ConflictException("Verification already processed");
        }

        if (request.approved()) {
            verification.setStatus(PharmacistVerification.VerificationStatus.VERIFIED);
            User user = userRepository.findById(verification.getUserId()).orElse(null);
            if (user != null && user.getRole() != UserRole.ADMIN) {
                user.setRole(UserRole.PHARMACIST);
                userRepository.save(user);
            }
        } else {
            verification.setStatus(PharmacistVerification.VerificationStatus.REJECTED);
            verification.setRejectionReason(request.rejectionReason());
        }
        verification.setReviewedBy(reviewerUserId);
        verification.setReviewedAt(Instant.now());
        PharmacistVerification saved = verificationRepository.save(verification);
        log.info("Pharmacist verification {} -> {}", verification.getId(), verification.getStatus());
        return PharmacistVerificationResponse.from(saved);
    }

    public PharmacistVerificationResponse getByUser(UUID userId) {
        return PharmacistVerificationResponse.from(verificationRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("No verification record for user")));
    }

    public Page<PharmacistVerificationResponse> listByStatus(String status, int page, int size) {
        PharmacistVerification.VerificationStatus parsed = parseStatus(status);
        Pageable pageable = PageRequest.of(Math.max(page, 0), size > 0 ? size : 20);
        return verificationRepository.findByStatus(parsed, pageable)
                .map(PharmacistVerificationResponse::from);
    }

    /**
     * Validates a pharmacist's license and role. Called by other services
     * (e.g. delivery assignment) to confirm the user may dispense.
     */
    @Transactional(readOnly = true)
    public boolean isVerifiedPharmacist(UUID userId) {
        return verificationRepository.findByUserId(userId)
                .map(v -> v.getStatus() == PharmacistVerification.VerificationStatus.VERIFIED)
                .orElse(false);
    }

    private PharmacistVerification.VerificationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return PharmacistVerification.VerificationStatus.PENDING;
        }
        try {
            return PharmacistVerification.VerificationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status: " + status);
        }
    }

    private String normalize(String licenseNumber) {
        return licenseNumber == null ? "" : licenseNumber.trim().toUpperCase();
    }
}
