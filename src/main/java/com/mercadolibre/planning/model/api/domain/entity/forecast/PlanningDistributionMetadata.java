package com.mercadolibre.planning.model.api.domain.entity.forecast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;

@Entity
@IdClass(PlanningDistributionMetadataId.class)
@Builder
@AllArgsConstructor
@Data
public class PlanningDistributionMetadata {

    @Id
    @JoinColumn(name = "planning_distribution_id")
    private long planningDistributionId;

    @Id
    @Column(name = "`key`")
    private String key;

    @Column(name = "`value`")
    private String value;
}
