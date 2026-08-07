package com.medikit.health.repository;

import com.medikit.health.entity.SymptomSynonym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SymptomSynonymRepository extends JpaRepository<SymptomSynonym, Long> {

    List<SymptomSynonym> findByAliasIn(Collection<String> aliases);
}
