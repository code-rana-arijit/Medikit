package com.medikit.prescription.service;

import com.medikit.common.audit.AuditService;
import com.medikit.common.event.EventPublisher;
import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.ConflictException;
import com.medikit.common.web.ForbiddenException;
import com.medikit.common.web.NotFoundException;
import com.medikit.prescription.consumer.PrescriptionEventConsumer;
import com.medikit.prescription.dto.PrescriptionResponse;
import com.medikit.prescription.dto.PrescriptionUploadRequest;
import com.medikit.prescription.dto.ValidationRequest;
import com.medikit.prescription.entity.Prescription;
import com.medikit.prescription.entity.PrescriptionStatus;
import com.medikit.prescription.entity.PrescriptionValidation;
import com.medikit.prescription.entity.ValidationDecision;
import com.medikit.prescription.repository.PrescriptionRepository;
import com.medikit.prescription.repository.PrescriptionValidationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
public class PrescriptionService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionService.class);
    private static final long VALIDITY_DAYS = 30;

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionValidationRepository validationRepository;
    private final EventPublisher eventPublisher;
    private final AuditService auditService;

    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                               PrescriptionValidationRepository validationRepository,
                               EventPublisher eventPublisher,
                               AuditService auditService) {
        this.prescriptionRepository = prescriptionRepository;
        this.validationRepository = validationRepository;
        this.eventPublisher = eventPublisher;
        this.auditService = auditService;
    }

    @Transactional
    public PrescriptionResponse upload(UUID userId, PrescriptionUploadRequest request) {
        Prescription prescription = Prescription.builder()
                .userId(userId)
                .patientName(request.patientName())
                .patientAge(request.patientAge())
                .doctorName(request.doctorName())
                .diagnosis(request.diagnosis())
                .status(PrescriptionStatus.UPLOADED)
                .imageUrl(request.imageUrl())
                .build();
        return PrescriptionResponse.from(prescriptionRepository.save(prescription));
    }

    public Page<PrescriptionResponse> listByUser(UUID userId, int page, int size) {
        return prescriptionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(PrescriptionResponse::from);
    }

    public PrescriptionResponse get(UUID id) {
        PrescriptionResponse response = PrescriptionResponse.from(getEntity(id));
        auditService.record(AuditService.AuditAction.PRESCRIPTION_ACCESSED, null, "SYSTEM",
                "prescription", id.toString(), null);
        return response;
    }

    @Transactional
    public PrescriptionResponse submitForValidation(UUID userId, UUID prescriptionId) {
        Prescription prescription = getEntity(prescriptionId);
        if (!prescription.getUserId().equals(userId)) {
            throw new ForbiddenException("Cannot submit another user's prescription");
        }
        if (prescription.getStatus() != PrescriptionStatus.UPLOADED) {
            throw new ConflictException("Only prescriptions in UPLOADED status can be submitted");
        }
        prescription.setStatus(PrescriptionStatus.PENDING_VALIDATION);
        return PrescriptionResponse.from(prescriptionRepository.save(prescription));
    }

    @Transactional
    public PrescriptionResponse validate(UUID prescriptionId, ValidationRequest request) {
        Prescription prescription = getEntity(prescriptionId);
        if (prescription.getStatus() != PrescriptionStatus.PENDING_VALIDATION) {
            throw new ConflictException("Prescription is not pending validation");
        }
        if (request.decision() == ValidationDecision.REJECTED
                && (request.rejectionReason() == null || request.rejectionReason().isBlank())) {
            throw new BadRequestException("Rejection reason is required when rejecting a prescription");
        }

        PrescriptionValidation validation = PrescriptionValidation.builder()
                .prescriptionId(prescriptionId)
                .validatorId(request.validatorId())
                .decision(request.decision())
                .comments(request.comments())
                .build();
        validationRepository.save(validation);

        if (request.decision() == ValidationDecision.APPROVED) {
            Instant now = Instant.now();
            prescription.setStatus(PrescriptionStatus.APPROVED);
            prescription.setApprovedBy(request.validatorId());
            prescription.setApprovedAt(now);
            prescription.setExpiresAt(now.plus(VALIDITY_DAYS, ChronoUnit.DAYS));
            prescription.setRejectionReason(null);
        } else {
            prescription.setStatus(PrescriptionStatus.REJECTED);
            prescription.setRejectionReason(request.rejectionReason());
            prescription.setApprovedBy(null);
            prescription.setApprovedAt(null);
            prescription.setExpiresAt(null);
        }

        Prescription saved = prescriptionRepository.save(prescription);
        publishValidated(saved);

        auditService.record(AuditService.AuditAction.ADMIN_REVIEW,
                request.validatorId() == null ? null : request.validatorId().toString(),
                "PHARMACIST",
                "prescription", prescriptionId.toString(), Map.of(
                        "decision", request.decision().name(),
                        "expiresAt", String.valueOf(saved.getExpiresAt())));

        return PrescriptionResponse.from(saved);
    }

    private Prescription getEntity(UUID id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Prescription not found: " + id));
    }

    private void publishValidated(Prescription prescription) {
        try {
            eventPublisher.publish(PrescriptionEventConsumer.PRESCRIPTION_VALIDATED,
                    prescription.getId().toString(),
                    Map.of(
                            "prescriptionId", prescription.getId(),
                            "status", prescription.getStatus().name(),
                            "userId", prescription.getUserId()
                    ));
        } catch (Exception e) {
            // Non-blocking event publish failure
            log.warn("Failed to publish prescription validated event for {}", prescription.getId(), e);
        }
    }
}
