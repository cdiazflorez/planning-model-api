package com.mercadolibre.planning.model.api.domain.usecase.simulation;

import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.QuantityByDate;
import lombok.Value;

import java.util.List;

@Value
public class SimulationInputEntity {

    private EntityType entityType;
    private List<QuantityByDate> values;
}
