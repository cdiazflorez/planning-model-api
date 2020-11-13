package com.mercadolibre.planning.model.api.domain.usecase.input;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Value
@Builder
public class GetEntityInput {

    private String warehouseId;
    private Workflow workflow;
    private EntityType entityType;
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateTo;
    private Source source;
    private List<ProcessName> processName;
    private Set<ProcessingType> processingType;
    private List<Simulation> simulations;

    //TODO: Sacar este atributo a una clase aparte
    private Set<Integer> abilityLevel;
}
