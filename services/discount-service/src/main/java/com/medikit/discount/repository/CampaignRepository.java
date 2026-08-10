package com.medikit.discount.repository;

import com.medikit.discount.entity.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, UUID> {
}
