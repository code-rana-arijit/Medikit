package com.medikit.prescription.service;

import com.medikit.prescription.entity.Prescription;
import com.medikit.prescription.entity.PrescriptionStatus;
import com.medikit.prescription.repository.PrescriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExpiryScheduler.class);
    private static final long EXPIRY_DAYS = 30;

    private final PrescriptionRepository prescriptionRepository;

    public ExpiryScheduler(PrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireApprovedPrescriptions() {
        Instant cutoff = Instant.now().minus(EXPIRY_DAYS, ChronoUnit.DAYS);
        List<Prescription> expired = prescriptionRepository
                .findByStatusAndCreatedAtBefore(PrescriptionStatus.APPROVED, cutoff);
        for (Prescription prescription : expired) {
            prescription.setStatus(PrescriptionStatus.EXPIRED);
            prescriptionRepository.save(prescription);
            log.info("Prescription {} expired", prescription.getId());
        }
    }
}
