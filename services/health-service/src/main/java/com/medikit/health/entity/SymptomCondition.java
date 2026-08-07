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
        name = "symptom_conditions",
        indexes = {
                @Index(name = "idx_symptom_conditions_symptom", columnList = "symptom"),
                @Index(name = "idx_symptom_conditions_condition", columnList = "condition_name")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymptomCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "symptom", nullable = false, length = 120)
    private String symptom;

    @Column(name = "condition_name", nullable = false, length = 120)
    private String conditionName;

    @Column(name = "weight", nullable = false)
    private double weight;

    @Column(name = "urgent", nullable = false)
    private boolean urgent;

    @Column(name = "referral_note", length = 500)
    private String referralNote;
}
