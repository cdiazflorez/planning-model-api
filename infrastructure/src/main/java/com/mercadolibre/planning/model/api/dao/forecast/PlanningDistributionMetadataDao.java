package com.mercadolibre.planning.model.api.dao.forecast;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

@Entity
@IdClass(PlanningDistributionMetadataDaoId.class)
@Getter
@Builder
@AllArgsConstructor
@Table(name = "planning_distribution_metadata")
public class PlanningDistributionMetadataDao {

    @Id
    @JoinColumn(name = "planning_distribution_id")
    private long planningDistributionId;

    @Id
    @Column(name = "`key`")
    private String key;

    @Column(name = "`value`")
    private String value;
}
