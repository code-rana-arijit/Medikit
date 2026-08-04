package com.medikit.prescription.service;

import com.medikit.common.event.EventPublisher;
import com.medikit.prescription.dto.PrescriptionResponse;
import com.medikit.prescription.entity.Prescription;
import com.medikit.prescription.entity.PrescriptionStatus;
import com.medikit.prescription.repository.PrescriptionRepository;
import com.medikit.prescription.repository.PrescriptionValidationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private PrescriptionValidationRepository validationRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PrescriptionService prescriptionService;

    @Test
    void submitForValidation_movesUploadedToPendingValidation() {
        UUID userId = UUID.randomUUID();
        UUID prescriptionId = UUID.randomUUID();
        Prescription prescription = Prescription.builder()
                .id(prescriptionId)
                .userId(userId)
                .patientName("John Doe")
                .patientAge(30)
                .status(PrescriptionStatus.UPLOADED)
                .build();

        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(prescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse response = prescriptionService.submitForValidation(userId, prescriptionId);

        assertThat(prescription.getStatus()).isEqualTo(PrescriptionStatus.PENDING_VALIDATION);
        assertThat(response.status()).isEqualTo(PrescriptionStatus.PENDING_VALIDATION.name());
        verify(prescriptionRepository).save(prescription);
    }
}
