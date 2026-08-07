package com.medikit.health.repository;

import com.medikit.health.entity.DrugInteraction;
import com.medikit.health.model.InteractionSeverity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrugInteractionRepository extends JpaRepository<DrugInteraction, Long> {

    List<DrugInteraction> findByDrugAInAndDrugBIn(List<String> drugANames, List<String> drugBNames);

    List<DrugInteraction> findBySeverity(InteractionSeverity severity);

    boolean existsByDrugAAndDrugB(String drugA, String drugB);
}
