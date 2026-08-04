package com.medikit.prescription.controller;

import com.medikit.common.security.UserContext;
import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.PageResult;
import com.medikit.prescription.dto.PrescriptionResponse;
import com.medikit.prescription.dto.PrescriptionUploadRequest;
import com.medikit.prescription.dto.ValidationRequest;
import com.medikit.prescription.service.PrescriptionService;
import com.medikit.prescription.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final StorageService storageService;

    public PrescriptionController(PrescriptionService prescriptionService, StorageService storageService) {
        this.prescriptionService = prescriptionService;
        this.storageService = storageService;
    }

    @PostMapping
    public ResponseEntity<PrescriptionResponse> upload(@Valid @RequestBody PrescriptionUploadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prescriptionService.upload(currentUserId(), request));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PrescriptionResponse> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam("patientName") String patientName,
            @RequestParam("patientAge") int patientAge,
            @RequestParam(required = false) String doctorName,
            @RequestParam(required = false) String diagnosis) {
        String imageUrl = storageService.store(file);
        PrescriptionUploadRequest request =
                new PrescriptionUploadRequest(patientName, patientAge, doctorName, diagnosis, imageUrl);
        return ResponseEntity.status(HttpStatus.CREATED).body(prescriptionService.upload(currentUserId(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<PageResult<PrescriptionResponse>> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PrescriptionResponse> result =
                prescriptionService.listByUser(currentUserId(), page, Math.min(size, 50));
        return ResponseEntity.ok(PageResult.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PrescriptionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(prescriptionService.get(id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<PrescriptionResponse> submit(@PathVariable UUID id) {
        return ResponseEntity.ok(prescriptionService.submitForValidation(currentUserId(), id));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<PrescriptionResponse> validate(@PathVariable UUID id,
                                                         @Valid @RequestBody ValidationRequest request) {
        return ResponseEntity.ok(prescriptionService.validate(id, request));
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
