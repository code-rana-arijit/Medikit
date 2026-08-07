package com.medikit.health.entity;

import com.medikit.health.model.InteractionSeverity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "drug_interactions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_drug_pair",
                columnNames = {"drug_a", "drug_b"}),
        indexes = {
                @Index(name = "idx_interactions_drug_a", columnList = "drug_a"),
                @Index(name = "idx_interactions_drug_b", columnList = "drug_b")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugInteraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drug_a", nullable = false, length = 120)
    private String drugA;

    @Column(name = "drug_b", nullable = false, length = 120)
    private String drugB;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private InteractionSeverity severity;

    @Column(name = "effect", nullable = false, length = 500)
    private String effect;

    @Column(name = "recommendation", nullable = false, length = 500)
    private String recommendation;

    @Column(name = "source", length = 200)
    private String source;
}
