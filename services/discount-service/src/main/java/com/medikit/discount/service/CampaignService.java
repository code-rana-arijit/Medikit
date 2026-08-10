package com.medikit.discount.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.common.web.NotFoundException;
import com.medikit.discount.dto.CampaignResponse;
import com.medikit.discount.dto.CreateCampaignRequest;
import com.medikit.discount.dto.DiscountCodeResponse;
import com.medikit.discount.entity.Campaign;
import com.medikit.discount.entity.DiscountCode;
import com.medikit.discount.model.DiscountType;
import com.medikit.discount.repository.CampaignRepository;
import com.medikit.discount.repository.DiscountCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CampaignService {

    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepository campaignRepository;
    private final DiscountCodeRepository discountCodeRepository;

    public CampaignService(CampaignRepository campaignRepository,
                           DiscountCodeRepository discountCodeRepository) {
        this.campaignRepository = campaignRepository;
        this.discountCodeRepository = discountCodeRepository;
    }

    @Transactional
    public CampaignResponse create(CreateCampaignRequest request) {
        validateType(request);
        Campaign campaign = Campaign.builder()
                .name(request.name())
                .description(request.description())
                .discountType(request.discountType())
                .discountAmount(request.discountType() == DiscountType.FIXED ? request.discountAmount() : null)
                .percentage(request.discountType() == DiscountType.PERCENTAGE ? request.percentage() : null)
                .validForDays(request.validForDays())
                .totalCodes(request.totalCodes())
                .issuedCodes(0)
                .active(true)
                .firstOrderOnly(request.firstOrderOnly())
                .createdAt(Instant.now())
                .build();
        Campaign saved = campaignRepository.save(campaign);

        int issued = 0;
        for (int i = 0; i < request.totalCodes(); i++) {
            DiscountCode code = DiscountCode.builder()
                    .code(generateCode())
                    .userId(null)
                    .discountType(request.discountType())
                    .discountAmount(request.discountType() == DiscountType.FIXED ? request.discountAmount() : null)
                    .percentage(request.discountType() == DiscountType.PERCENTAGE ? request.percentage() : null)
                    .campaignId(saved.getId())
                    .title(request.name())
                    .firstOrderOnly(request.firstOrderOnly())
                    .currency("INR")
                    .expiresAt(Instant.now().plusSeconds(request.validForDays() * 86400L))
                    .createdAt(Instant.now())
                    .build();
            discountCodeRepository.save(code);
            issued++;
        }
        saved.setIssuedCodes(issued);
        campaignRepository.save(saved);

        log.info("Created campaign {} with {} codes", saved.getId(), issued);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CampaignResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return campaignRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CampaignResponse get(UUID campaignId) {
        return toResponse(find(campaignId));
    }

    @Transactional(readOnly = true)
    public Page<DiscountCodeResponse> codes(UUID campaignId, int page, int size) {
        find(campaignId);
        Pageable pageable = PageRequest.of(page, Math.min(size, 50));
        return discountCodeRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId, pageable)
                .map(this::toCodeResponse);
    }

    private void validateType(CreateCampaignRequest request) {
        if (request.discountType() == DiscountType.FIXED && request.discountAmount() == null) {
            throw new BadRequestException("Fixed discount campaigns require a discount amount");
        }
        if (request.discountType() == DiscountType.PERCENTAGE && request.percentage() == null) {
            throw new BadRequestException("Percentage discount campaigns require a percentage");
        }
    }

    private Campaign find(UUID campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
    }

    private String generateCode() {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "PROMO-" + suffix;
    }

    private CampaignResponse toResponse(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getDiscountType().name(),
                campaign.getDiscountAmount(),
                campaign.getPercentage(),
                campaign.getValidForDays(),
                campaign.getTotalCodes(),
                campaign.getIssuedCodes(),
                campaign.isActive(),
                campaign.isFirstOrderOnly(),
                campaign.getCreatedAt());
    }

    private DiscountCodeResponse toCodeResponse(DiscountCode code) {
        return new DiscountCodeResponse(
                code.getCode(),
                code.getUserId(),
                code.getDiscountType().name(),
                code.getDiscountAmount(),
                code.getPercentage(),
                code.getCampaignId(),
                code.getTitle(),
                code.isFirstOrderOnly(),
                code.getCurrency(),
                code.getStatus().name(),
                code.getExpiresAt(),
                code.getCreatedAt(),
                code.getRedeemedAt(),
                code.getRedeemedOrderId());
    }
}
