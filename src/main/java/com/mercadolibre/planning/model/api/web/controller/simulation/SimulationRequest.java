package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.SlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.SimulationInput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;

import lombok.Data;

import javax.validation.constraints.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
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

    @NotNull
    private Long userId;

    @NotNull
    private String timeZone;

    private boolean applyDeviation;

    public SlaProjectionInput toProjectionInput(
            final Map<ZonedDateTime, Integer> capacity,
            final List<GetPlanningDistributionOutput> units,
            final Workflow workflow,
            final List<GetSlaByWarehouseOutput> cptByWarehouse) {

        return SlaProjectionInput.builder()
                .workflow(workflow)
                .logisticCenterId(warehouseId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .capacity(capacity)
                .planningUnits(units)
                .backlog(backlog.stream()
                        .map(QuantityByDate::toBacklog)
                        .collect(toList()))
                .slaByWarehouse(cptByWarehouse)
                .currentDate(getCurrentUtcDate())
                .build();
    }

    public SimulationInput toSimulationInput(final Workflow workflow) {
        return SimulationInput.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .processName(processName)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .backlog(backlog)
                .simulations(simulations)
                .userId(userId)
                .build();
    }

    public GetEntityInput toThroughputEntityInput(final Workflow workflow) {
        return GetEntityInput.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .entityType(THROUGHPUT)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .source(SIMULATION)
                .processName(processName)
                .simulations(simulations)
                .build();
    }

    public GetEntityInput toForecastedThroughputEntityInput(final Workflow workflow) {
        return GetEntityInput.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .entityType(THROUGHPUT)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .source(SIMULATION)
                .processName(processName)
                .build();
    }

    public GetPlanningDistributionInput toPlanningInput(final Workflow workflow) {
        return GetPlanningDistributionInput.builder()
                .warehouseId(warehouseId)
                .workflow(workflow)
                .dateOutFrom(dateFrom)
                .dateOutTo(dateTo)
                .applyDeviation(applyDeviation)
                .build();
    }
}
