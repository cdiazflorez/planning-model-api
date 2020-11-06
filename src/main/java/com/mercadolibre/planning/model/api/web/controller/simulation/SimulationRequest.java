package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.SimulationInput;
import com.mercadolibre.planning.model.api.web.controller.request.QuantityByDate;
import lombok.Data;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static java.util.stream.Collectors.toList;

@Data
public class SimulationRequest {

    @NotNull
    private String warehouseId;

    @NotNull
    private List<ProcessName> processName;

    @NotNull
    private ZonedDateTime dateFrom;

    @NotNull
    private ZonedDateTime dateTo;

    @NotNull
    private List<QuantityByDate> backlog;

    @NotNull
    private List<Simulation> simulations;

    public ProjectionInput toProjectionInput(final List<EntityOutput> throughputs,
                                             final List<GetPlanningDistributionOutput> units) {
        return ProjectionInput.builder()
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .throughput(throughputs)
                .planningUnits(units)
                .backlog(backlog.stream()
                        .map(QuantityByDate::toBacklog)
                        .collect(toList()))
                .build();
    }

    public SimulationInput toSimulationInput(final Workflow workflow) {
        return new SimulationInput(warehouseId, workflow, processName,
                dateFrom, dateTo, backlog, simulations);
    }

    public GetEntityInput toThroughputEntityInput(final Workflow workflow) {
        return new GetEntityInput(warehouseId, workflow, THROUGHPUT,
                dateFrom, dateTo, SIMULATION, processName, Set.of(ProcessingType.ACTIVE_WORKERS));
    }

    public GetPlanningDistributionInput toPlanningInput(final Workflow workflow) {
        return GetPlanningDistributionInput.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .dateOutFrom(dateFrom)
                .dateOutTo(dateTo)
                .build();
    }
}
