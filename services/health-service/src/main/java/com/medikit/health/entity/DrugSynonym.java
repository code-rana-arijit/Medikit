package com.medikit.health.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "drug_synonyms",
        indexes = {
                @Index(name = "idx_synonyms_canonical", columnList = "canonical_name")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugSynonym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alias", nullable = false, length = 120, unique = true)
    private String alias;

    @Column(name = "canonical_name", nullable = false, length = 120)
    private String canonicalName;
}
