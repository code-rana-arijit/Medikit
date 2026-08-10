package com.medikit.discount.service;

import com.medikit.common.web.BadRequestException;
import com.medikit.discount.dto.CampaignResponse;
import com.medikit.discount.dto.CreateCampaignRequest;
import com.medikit.discount.dto.DiscountCodeResponse;
import com.medikit.discount.entity.Campaign;
import com.medikit.discount.model.DiscountType;
import com.medikit.discount.repository.CampaignRepository;
import com.medikit.discount.repository.DiscountCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private DiscountCodeRepository discountCodeRepository;

    private CampaignService campaignService;

    @Captor
    private ArgumentCaptor<Campaign> campaignCaptor;

    private final UUID campaignId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        campaignService = new CampaignService(campaignRepository, discountCodeRepository);
    }

    @Test
    void createFixedCampaignIssuesBatchOfCodes() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                "Flat 50 off", "welcome deal", DiscountType.FIXED,
                new BigDecimal("50"), null, 30, 5, false);

        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> {
            Campaign c = inv.getArgument(0);
            c.setId(campaignId);
            return c;
        });

        CampaignResponse response = campaignService.create(request);

        assertThat(response.issuedCodes()).isEqualTo(5);
        assertThat(response.totalCodes()).isEqualTo(5);
        assertThat(response.discountAmount()).isEqualByComparingTo("50");
        verify(discountCodeRepository, times(5)).save(any());
    }

    @Test
    void createPercentageCampaignRequiresPercentage() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                "10% off", null, DiscountType.PERCENTAGE,
                null, new BigDecimal("10"), 15, 3, true);

        when(campaignRepository.save(any(Campaign.class))).thenAnswer(inv -> {
            Campaign c = inv.getArgument(0);
            c.setId(campaignId);
            return c;
        });

        CampaignResponse response = campaignService.create(request);

        assertThat(response.discountType()).isEqualTo(DiscountType.PERCENTAGE.name());
        assertThat(response.percentage()).isEqualByComparingTo("10");
        assertThat(response.firstOrderOnly()).isTrue();
        verify(discountCodeRepository, times(3)).save(any());
    }

    @Test
    void createRejectsPercentageCampaignWithoutPercentage() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                "10% off", null, DiscountType.PERCENTAGE,
                null, null, 15, 3, false);

        assertThatThrownBy(() -> campaignService.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("percentage");
    }

    @Test
    void getReturnsCampaign() {
        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign()));

        CampaignResponse response = campaignService.get(campaignId);

        assertThat(response.id()).isEqualTo(campaignId);
        assertThat(response.name()).isEqualTo("Flat 50 off");
    }

    @Test
    void listReturnsCampaignsPage() {
        when(campaignRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(campaign())));

        Page<CampaignResponse> page = campaignService.list(0, 20);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).id()).isEqualTo(campaignId);
    }

    private Campaign campaign() {
        return Campaign.builder()
                .id(campaignId)
                .name("Flat 50 off")
                .discountType(DiscountType.FIXED)
                .discountAmount(new BigDecimal("50"))
                .validForDays(30)
                .totalCodes(5)
                .issuedCodes(5)
                .active(true)
                .firstOrderOnly(false)
                .createdAt(Instant.now())
                .build();
    }
}
