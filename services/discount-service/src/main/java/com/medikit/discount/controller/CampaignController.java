package com.medikit.discount.controller;

import com.medikit.common.web.PageResult;
import com.medikit.discount.dto.CampaignResponse;
import com.medikit.discount.dto.CreateCampaignRequest;
import com.medikit.discount.dto.DiscountCodeResponse;
import com.medikit.discount.service.CampaignService;
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
@RequestMapping("/api/v1/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(campaignService.create(request));
    }

    @GetMapping
    public ResponseEntity<PageResult<CampaignResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CampaignResponse> result = campaignService.list(page, Math.min(size, 50));
        return ResponseEntity.ok(PageResult.from(result));
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> get(@PathVariable UUID campaignId) {
        return ResponseEntity.ok(campaignService.get(campaignId));
    }

    @GetMapping("/{campaignId}/codes")
    public ResponseEntity<PageResult<DiscountCodeResponse>> codes(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DiscountCodeResponse> result = campaignService.codes(campaignId, page, Math.min(size, 50));
        return ResponseEntity.ok(PageResult.from(result));
    }
}
