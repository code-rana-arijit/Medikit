package com.medikit.health.repository;

import com.medikit.health.entity.ConditionRemedy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ConditionRemedyRepository extends JpaRepository<ConditionRemedy, Long> {

    List<ConditionRemedy> findByConditionNameIn(Collection<String> conditionNames);

    boolean existsByConditionNameAndMedicine(String conditionName, String medicine);
}
