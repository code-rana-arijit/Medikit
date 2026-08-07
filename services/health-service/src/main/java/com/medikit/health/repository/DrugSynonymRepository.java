package com.medikit.health.repository;

import com.medikit.health.entity.DrugSynonym;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DrugSynonymRepository extends JpaRepository<DrugSynonym, Long> {

    List<DrugSynonym> findByAliasIn(Collection<String> aliases);

    boolean existsByAlias(String alias);
}
