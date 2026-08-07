package com.medikit.product.service;

import com.medikit.common.web.ConflictException;
import com.medikit.common.web.NotFoundException;
import com.medikit.product.dto.PharmacyResponse;
import com.medikit.product.entity.Pharmacy;
import com.medikit.product.repository.PharmacyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PharmacyService {

    private static final Logger log = LoggerFactory.getLogger(PharmacyService.class);

    private final PharmacyRepository pharmacyRepository;
    private final DrugLicenseValidator licenseValidator;

    public PharmacyService(PharmacyRepository pharmacyRepository,
                           DrugLicenseValidator licenseValidator) {
        this.pharmacyRepository = pharmacyRepository;
        this.licenseValidator = licenseValidator;
    }

    public Page<PharmacyResponse> list(int page, int size) {
        return pharmacyRepository.findAll(PageRequest.of(Math.max(page, 0), size > 0 ? size : 20))
                .map(PharmacyResponse::from);
    }

    public PharmacyResponse get(UUID id) {
        return PharmacyResponse.from(pharmacyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pharmacy not found: " + id)));
    }

    @Transactional
    public PharmacyResponse create(PharmacyRegistration request) {
        pharmacyRepository.findByLicenseNumber(request.licenseNumber()).ifPresent(existing -> {
            throw new ConflictException("Pharmacy with this license number already registered");
        });

        Pharmacy.LicenseStatus status = licenseValidator.validate(
                request.licenseNumber(), request.licenseState(), request.licenseExpiry());

        Pharmacy pharmacy = Pharmacy.builder()
                .name(request.name())
                .licenseNumber(request.licenseNumber().trim().toUpperCase())
                .licenseState(request.licenseState().toUpperCase())
                .licenseExpiry(request.licenseExpiry())
                .address(request.address())
                .phone(request.phone())
                .ownerUserId(request.ownerUserId())
                .licenseStatus(status)
                .active(status == Pharmacy.LicenseStatus.VALID)
                .build();

        Pharmacy saved = pharmacyRepository.save(pharmacy);
        log.info("Registered pharmacy {} with license status {}",
                saved.getId(), saved.getLicenseStatus());
        return PharmacyResponse.from(saved);
    }

    /**
     * Re-validates a pharmacy's license, e.g. on a periodic compliance sweep.
     */
    @Transactional
    public PharmacyResponse revalidate(UUID id) {
        Pharmacy pharmacy = pharmacyRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pharmacy not found: " + id));
        Pharmacy.LicenseStatus status = licenseValidator.validate(
                pharmacy.getLicenseNumber(), pharmacy.getLicenseState(), pharmacy.getLicenseExpiry());
        pharmacy.setLicenseStatus(status);
        pharmacy.setActive(status == Pharmacy.LicenseStatus.VALID);
        return PharmacyResponse.from(pharmacyRepository.save(pharmacy));
    }

    public record PharmacyRegistration(
            String name,
            String licenseNumber,
            String licenseState,
            java.time.LocalDate licenseExpiry,
            String address,
            String phone,
            UUID ownerUserId
    ) {
    }
}
