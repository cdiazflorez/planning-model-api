package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.GetForecastedThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.ProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.ActivateSimulationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.GetSimulationThroughputUseCase;
import com.mercadolibre.planning.model.api.web.controller.editor.EntityTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
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

import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/simulations")
public class SimulationController {

    private final ActivateSimulationUseCase activateSimulationUseCase;
    private final CalculateCptProjectionUseCase calculateCptProjectionUseCase;
    private final GetForecastedThroughputUseCase getForecastedThroughputUseCase;
    private final GetPlanningDistributionUseCase getPlanningDistributionUseCase;
    private final GetSimulationThroughputUseCase getSimulationThroughputUseCase;

    @PostMapping("/save")
    @Trace(dispatcher = true)
    public ResponseEntity<List<SaveSimulationResponse>> saveSimulation(
            @PathVariable final Workflow workflow,
            @Valid @RequestBody final SimulationRequest request) {

        activateSimulationUseCase.execute(request.toSimulationInput(workflow));

        final List<EntityOutput> throughput = getForecastedThroughputUseCase
                .execute(request.toThroughputEntityInput(workflow));

        return ResponseEntity.ok(projectSimulation(request, workflow, throughput).stream()
                .map(SaveSimulationResponse::fromProjectionOutput)
                .collect(toList()));
    }

    @PostMapping("/run")
    @Trace(dispatcher = true)
    public ResponseEntity<List<SaveSimulationResponse>> runSimulation(
            @PathVariable final Workflow workflow,
            @Valid @RequestBody final SimulationRequest request) {

        final List<EntityOutput> throughput = getSimulationThroughputUseCase
                .execute(request.toThroughputEntityInput(workflow));

        return ResponseEntity.ok(projectSimulation(request, workflow, throughput).stream()
                .map(SaveSimulationResponse::fromProjectionOutput)
                .collect(toList()));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(EntityType.class, new EntityTypeEditor());
        dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
    }

    private List<ProjectionOutput> projectSimulation(final SimulationRequest request,
                                                     final Workflow workflow,
                                                     final List<EntityOutput> throughput) {

        final List<GetPlanningDistributionOutput> planningDistributions =
                getPlanningDistributionUseCase.execute(request.toPlanningInput(workflow));

        return calculateCptProjectionUseCase
                .execute(request.toProjectionInput(throughput, planningDistributions));
    }
}
