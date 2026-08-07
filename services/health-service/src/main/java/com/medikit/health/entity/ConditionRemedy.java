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
        name = "condition_remedies",
        indexes = {
                @Index(name = "idx_condition_remedies_condition", columnList = "condition_name")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionRemedy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "condition_name", nullable = false, length = 120)
    private String conditionName;

    @Column(name = "medicine", nullable = false, length = 120)
    private String medicine;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "otc", nullable = false)
    private boolean otc;

    @Column(name = "usage_note", length = 300)
    private String usageNote;
}
