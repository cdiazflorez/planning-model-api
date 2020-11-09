package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Data
@AllArgsConstructor
public class Simulation {

    @NotNull
    private ProcessName processName;

    @NotNull
    private List<SimulationEntity> entities;


    public List<EntityOutput> toEntityOutputs(final Workflow workflow,
                                              final EntityType entityType) {
        return entities.stream()
                .filter(simulationEntity -> simulationEntity.getType() == entityType)
                .map(simulationEntity -> simulationEntity.getValues()
                        .stream()
                        .map(value -> EntityOutput.builder()
                                .date(value.getDate())
                                .metricUnit(simulationEntity.getType().getMetricUnit())
                                .processName(processName)
                                .source(Source.SIMULATION)
                                .value(value.getQuantity())
                                .workflow(workflow)
                                .build())
                        .collect(toList()))
                .flatMap(List::stream)
                .collect(toList());
    }
}
