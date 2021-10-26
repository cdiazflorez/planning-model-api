package com.mercadolibre.planning.model.api.web.controller.projection;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeliveryPromiseProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.web.controller.editor.ProjectionTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.projection.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CptProjectionRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput.fromEntityOutputs;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.CPT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@SuppressWarnings("PMD.ExcessiveImports")
@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/projections")

public class ProjectionController {

    private final CalculateCptProjectionUseCase calculateCptProjection;
    private final CalculateBacklogProjectionUseCase calculateBacklogProjection;
    private final GetDeliveryPromiseProjectionUseCase delPromiseProjection;

    private final GetThroughputUseCase getThroughputUseCase;
    private final GetPlanningDistributionUseCase getPlanningUseCase;
    private final GetCapacityPerHourUseCase getCapacityPerHourUseCase;
    private final GetCycleTimeUseCase getCycleTimeUseCase;

    @PostMapping("/cpts")
    @Trace(dispatcher = true)
    public ResponseEntity<List<CptProjectionOutput>> getCptProjection(
            @PathVariable final Workflow workflow,
            @Valid @RequestBody final CptProjectionRequest request) {

        return generateCptProjection(workflow, request);
    }

    @PostMapping("/cpts/delivery_promise")
    @Trace(dispatcher = true)
    public ResponseEntity<List<CptProjectionOutput>> getDeliveryPromiseProjection(
            @PathVariable final Workflow workflow,
            @Valid @RequestBody final CptProjectionRequest request) {

        return ResponseEntity.ok(delPromiseProjection.execute(GetDeliveryPromiseProjectionInput
                .builder()
                .warehouseId(request.getWarehouseId())
                .workflow(workflow)
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .backlog(getBacklog(request.getBacklog()))
                .build())
        );
    }

    @PostMapping("/backlogs")
    @Trace(dispatcher = true)
    public ResponseEntity<List<BacklogProjectionOutput>> getBacklogProjections(
            @PathVariable final Workflow workflow,
            @Valid @RequestBody final BacklogProjectionRequest request) {

        final String warehouseId = request.getWarehouseId();
        final ZonedDateTime dateFrom = request.getDateFrom();
        final ZonedDateTime dateTo = request.getDateTo();

        final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput
                .builder()
                .warehouseId(warehouseId)
                .dateFrom(dateFrom.minusHours(1))
                .dateTo(dateTo)
                .source(SIMULATION)
                .processName(request.getProcessName())
                .workflow(workflow)
                .build());

        final List<GetPlanningDistributionOutput> planningUnits = getPlanningUseCase.execute(
                GetPlanningDistributionInput.builder()
                        .workflow(workflow)
                        .warehouseId(request.getWarehouseId())
                        .dateInTo(request.getDateTo().plusDays(1))
                        .dateOutFrom(request.getDateFrom())
                        .dateOutTo(request.getDateTo().plusDays(1))
                        .applyDeviation(request.isApplyDeviation())
                        .build());

        return ResponseEntity
                .ok(calculateBacklogProjection.execute(BacklogProjectionInput.builder()
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .throughputs(throughput)
                        .currentBacklogs(request.getCurrentBacklog())
                        .processNames(request.getProcessName())
                        .planningUnits(planningUnits)
                        .build()));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(ProjectionType.class, new ProjectionTypeEditor());
    }

    private ResponseEntity<List<CptProjectionOutput>> generateCptProjection(
            final Workflow workflow,
            final CptProjectionRequest request) {

        final String warehouseId = request.getWarehouseId();
        final ZonedDateTime dateFrom = request.getDateFrom();
        final ZonedDateTime dateTo = request.getDateTo();

        final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput
                .builder()
                .warehouseId(warehouseId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .processName(request.getProcessName())
                .workflow(workflow)
                .build());

        final Map<ZonedDateTime, Integer> capacity = getCapacityPerHourUseCase
                .execute(fromEntityOutputs(throughput))
                .stream()
                .collect(toMap(
                        CapacityOutput::getDate,
                        capacityOutput -> (int) capacityOutput.getValue()
                ));

        final List<GetPlanningDistributionOutput> planningUnits = getPlanningUseCase.execute(
                GetPlanningDistributionInput.builder()
                        .workflow(workflow)
                        .warehouseId(request.getWarehouseId())
                        .dateOutFrom(request.getDateFrom())
                        .dateOutTo(request.getDateTo())
                        .applyDeviation(request.isApplyDeviation())
                        .build());

        final List<CptProjectionOutput> cptProjectionOutputs =
                calculateCptProjection.execute(CptProjectionInput.builder()
                        .workflow(workflow)
                        .logisticCenterId(warehouseId)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .capacity(capacity)
                        .backlog(getBacklog(request.getBacklog()))
                        .planningUnits(planningUnits)
                        .projectionType(CPT)
                        .configurationByDateOut(getCtByDateOut(warehouseId, planningUnits))
                        .build());

        return ResponseEntity.ok(cptProjectionOutputs);
    }

    private List<Backlog> getBacklog(final List<QuantityByDate> backlogs) {
        return backlogs == null
                ? emptyList()
                : backlogs.stream().map(QuantityByDate::toBacklog).collect(toList());
    }

    private Map<ZonedDateTime, Configuration> getCtByDateOut(
            final String warehouseId,
            final List<GetPlanningDistributionOutput> planningUnits) {

        return getCycleTimeUseCase.execute(
                new GetCycleTimeInput(warehouseId, planningUnits.stream()
                        .map(GetPlanningDistributionOutput::getDateOut)
                        .collect(toList())));
    }
}
