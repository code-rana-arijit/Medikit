package com.medikit.health.repository;

import com.medikit.health.entity.SymptomCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SymptomConditionRepository extends JpaRepository<SymptomCondition, Long> {

    List<SymptomCondition> findBySymptomIn(Collection<String> symptoms);

    List<SymptomCondition> findByUrgentTrue();
}
