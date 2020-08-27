package com.mercadolibre.planning.model.api.domain.entity.forecast;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
public class PlanningDistributionMetadataEntityId implements Serializable {

    private static final long serialVersionUID = -1482279722548065637L;

    private long planningDistributionId;
    private String key;
}
