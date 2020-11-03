package com.mercadolibre.planning.model.api.web.controller;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetThroughputEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.CalculateProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.strategy.CalculateProjectionStrategy;
import com.mercadolibre.planning.model.api.exception.ProjectionTypeNotSupportedException;
import com.mercadolibre.planning.model.api.web.controller.editor.ProjectionTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.request.ProjectionRequest;
import com.mercadolibre.planning.model.api.web.controller.request.ProjectionType;
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

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("PMD.ExcessiveImports")
@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/projections")
public class ProjectionController {

    private final CalculateProjectionStrategy calculateProjectionStrategy;
    private final GetThroughputEntityUseCase getThroughputUseCase;
    private final GetPlanningDistributionUseCase getPlanningUseCase;

    @PostMapping
    @Trace(dispatcher = true)
    public ResponseEntity<List<ProjectionOutput>> getProjection(
            @PathVariable final Workflow workflow,
            @RequestBody final ProjectionRequest request) {

        final ProjectionType projectionType = request.getType();

        final CalculateProjectionUseCase calculateProjectionUseCase = calculateProjectionStrategy
                .getBy(projectionType)
                .orElseThrow(() -> new ProjectionTypeNotSupportedException(projectionType));

        final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput
                .builder()
                .warehouseId(request.getWarehouseId())
                .dateFrom(request.getDateFrom())
                .dateTo(request.getDateTo())
                .processName(request.getProcessName())
                .workflow(workflow)
                .build());

        final List<GetPlanningDistributionOutput> planningUnits = getPlanningUseCase.execute(
                GetPlanningDistributionInput.builder()
                        .workflow(workflow)
                        .warehouseId(request.getWarehouseId())
                        .dateOutFrom(request.getDateFrom())
                        .dateOutTo(request.getDateTo())
                        .build());

        return ResponseEntity
                .ok(calculateProjectionUseCase.execute(ProjectionInput.builder()
                        .dateFrom(request.getDateFrom())
                        .dateTo(request.getDateTo())
                        .backlog(getBacklog(request))
                        .throughput(throughput)
                        .planningUnits(planningUnits)
                        .build()));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(ProjectionType.class, new ProjectionTypeEditor());
    }

    private List<Backlog> getBacklog(final ProjectionRequest request) {
        return request.getBacklog() == null
                ? emptyList()
                : request.getBacklog().stream()
                .map(backlogRequest -> new Backlog(
                        backlogRequest.getDate(),
                        backlogRequest.getQuantity()))
                .collect(toList());
    }
}
