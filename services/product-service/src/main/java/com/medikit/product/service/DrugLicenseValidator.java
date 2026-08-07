package com.medikit.product.service;

import com.medikit.product.entity.Pharmacy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Drug retail license validation for pharmacies.
 * <p>
 * Validates license format, expiry, and (in production) would cross-check
 * against the state drug control registry via an outbound API. In this build
 * the registry check is simulated deterministically; the expiry and format
 * checks are always enforced.
 * </p>
 */
@Service
public class DrugLicenseValidator {

    private static final Logger log = LoggerFactory.getLogger(DrugLicenseValidator.class);
    private static final Pattern LICENSE_PATTERN = Pattern.compile("^[A-Z0-9]{5,20}$");
    private static final List<String> SUPPORTED_STATES = List.of(
            "MH", "DL", "KA", "TN", "AP", "TS", "GJ", "WB", "UP", "KL", "HR", "PB");

    public Pharmacy.LicenseStatus validate(String licenseNumber, String state, LocalDate expiry) {
        String normalized = licenseNumber == null ? "" : licenseNumber.trim().toUpperCase();
        if (!LICENSE_PATTERN.matcher(normalized).matches()) {
            return Pharmacy.LicenseStatus.REJECTED;
        }
        if (state == null || !SUPPORTED_STATES.contains(state.toUpperCase())) {
            return Pharmacy.LicenseStatus.REJECTED;
        }
        if (expiry == null || expiry.isBefore(LocalDate.now())) {
            return Pharmacy.LicenseStatus.EXPIRED;
        }
        // Simulated registry lookup: licenses ending in an even digit are
        // considered registered with the drug control authority.
        int lastDigit = Character.digit(normalized.charAt(normalized.length() - 1), 10);
        if (lastDigit >= 0 && lastDigit % 2 != 0) {
            log.warn("License {} not found in registry for state {}", normalized, state);
            return Pharmacy.LicenseStatus.REJECTED;
        }
        return Pharmacy.LicenseStatus.VALID;
    }
}
