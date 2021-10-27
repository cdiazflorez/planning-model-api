package com.mercadolibre.planning.model.api.web.controller.simulation;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.ActivateSimulationUseCase;
import com.mercadolibre.planning.model.api.web.controller.editor.EntityTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
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
import static com.mercadolibre.planning.model.api.web.controller.simulation.RunSimulationResponse.fromProjectionOutputs;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/simulations")
@SuppressWarnings("PMD.ExcessiveImports")
public class SimulationController {

    private final ActivateSimulationUseCase activateSimulationUseCase;
    private final CalculateCptProjectionUseCase calculateCptProjectionUseCase;
    private final GetThroughputUseCase getThroughputUseCase;
    private final GetPlanningDistributionUseCase getPlanningDistributionUseCase;
    private final GetCapacityPerHourUseCase getCapacityPerHourUseCase;
    private final GetCycleTimeUseCase getCycleTimeUseCase;

    @PostMapping("/save")
    @Trace(dispatcher = true)
    public ResponseEntity<List<SaveSimulationResponse>> saveSimulation(
            @PathVariable final Workflow workflow,
            @Valid @RequestBody final SimulationRequest request) {

        activateSimulationUseCase.execute(request.toSimulationInput(workflow));

        final List<EntityOutput> throughput = getThroughputUseCase
                .execute(request.toThroughputEntityInput(workflow));

        final Map<ZonedDateTime, Integer> capacity = getCapacityPerHourUseCase
                .execute(fromEntityOutputs(throughput))
                .stream()
                .collect(toMap(
                        CapacityOutput::getDate,
                        capacityOutput -> (int) capacityOutput.getValue()
                ));

        final List<GetPlanningDistributionOutput> planningDistributions =
                getPlanningDistributionUseCase.execute(request.toPlanningInput(workflow));

        final List<CptProjectionOutput> cptProjectionOutputs = calculateCptProjectionUseCase
                .execute(request.toProjectionInput(
                        capacity,
                        planningDistributions,
                        workflow,
                        getCycleTime(request, planningDistributions)));

        return ResponseEntity.ok(cptProjectionOutputs.stream()
                .map(SaveSimulationResponse::fromProjectionOutput)
                .collect(toList()));
    }

    @PostMapping("/run")
    @Trace(dispatcher = true)
    public ResponseEntity<List<RunSimulationResponse>> runSimulation(
            @PathVariable final Workflow workflow,
            @Valid @RequestBody final SimulationRequest request) {

        final List<GetPlanningDistributionOutput> planningDistributions =
                getPlanningDistributionUseCase.execute(request.toPlanningInput(workflow));

        final List<EntityOutput> simulatedThroughput = getThroughputUseCase
                .execute(request.toThroughputEntityInput(workflow));

        final Map<ZonedDateTime, Integer> simulatedCapacity = getCapacityPerHourUseCase
                .execute(fromEntityOutputs(simulatedThroughput))
                .stream()
                .collect(toMap(
                        CapacityOutput::getDate,
                        capacityOutput -> (int) capacityOutput.getValue()
                ));

        final List<CptProjectionOutput> projectSimulation = calculateCptProjectionUseCase
                .execute(request.toProjectionInput(
                        simulatedCapacity,
                        planningDistributions,
                        workflow,
                        getCycleTime(request, planningDistributions)));

        final List<EntityOutput> actualThroughput = getThroughputUseCase
                .execute(request.toForecastedThroughputEntityInput(workflow));

        final Map<ZonedDateTime, Integer> actualCapacity = getCapacityPerHourUseCase
                .execute(fromEntityOutputs(actualThroughput))
                .stream()
                .collect(toMap(
                        CapacityOutput::getDate,
                        capacityOutput -> (int) capacityOutput.getValue()
                ));

        final List<CptProjectionOutput> projection = calculateCptProjectionUseCase
                .execute(request.toProjectionInput(
                        actualCapacity,
                        planningDistributions,
                        workflow,
                        getCycleTime(request, planningDistributions)));

        return ResponseEntity.ok(fromProjectionOutputs(projectSimulation, projection));
    }

    public Map<ZonedDateTime, Configuration> getCycleTime(
            final SimulationRequest request,
            final List<GetPlanningDistributionOutput> planningDistributions) {

        return getCycleTimeUseCase
                .execute(new GetCycleTimeInput(request.getWarehouseId(),
                        planningDistributions.stream()
                                .map(GetPlanningDistributionOutput::getDateOut)
                                .collect(toList())));
    }

    @InitBinder
    public void initBinder(final PropertyEditorRegistry dataBinder) {
        dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
        dataBinder.registerCustomEditor(EntityType.class, new EntityTypeEditor());
        dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
    }
}
