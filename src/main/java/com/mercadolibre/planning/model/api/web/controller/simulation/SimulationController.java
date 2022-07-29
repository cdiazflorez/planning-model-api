package com.mercadolibre.planning.model.api.web.controller.simulation;

import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.CPT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static com.mercadolibre.planning.model.api.web.controller.simulation.RunSimulationResponse.fromProjectionOutputs;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetSlaProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetSlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.ActivateSimulationUseCase;
import com.mercadolibre.planning.model.api.web.controller.editor.EntityTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.newrelic.api.agent.Trace;
import java.util.List;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/simulations")
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LongVariable"})
public class SimulationController {

  private final ActivateSimulationUseCase activateSimulationUseCase;

  private final GetSlaProjectionUseCase getSlaProjectionUseCase;

  @PostMapping("/save")
  @Trace(dispatcher = true)
  public ResponseEntity<List<SaveSimulationResponse>> saveSimulation(
      @PathVariable final Workflow workflow,
      @Valid @RequestBody final SimulationRequest request) {

    activateSimulationUseCase.execute(request.toSimulationInput(workflow));

    final List<CptProjectionOutput> projections = getSlaProjectionUseCase.execute(
        new GetSlaProjectionInput(
            workflow,
            request.getWarehouseId(),
            CPT,
            request.getProcessName(),
            request.getDateFrom(),
            request.getDateTo(),
            request.getBacklog(),
            request.getTimeZone(),
            SIMULATION,
            request.getSimulations(),
            request.isApplyDeviation()
        )
    );

    return ResponseEntity.ok(projections.stream()
        .map(projection -> new SaveSimulationResponse(
            projection.getDate(),
            projection.getProjectedEndDate(),
            projection.getRemainingQuantity(),
            projection.getProcessingTime(),
            false))
        .collect(toList())
    );
  }

  @PostMapping("/run")
  @Trace(dispatcher = true)
  public ResponseEntity<List<RunSimulationResponse>> runSimulation(
      @PathVariable final Workflow workflow,
      @Valid @RequestBody final SimulationRequest request) {

    final List<CptProjectionOutput> simulatedProjections = getSlaProjectionUseCase.execute(
        new GetSlaProjectionInput(
            workflow,
            request.getWarehouseId(),
            CPT,
            request.getProcessName(),
            request.getDateFrom(),
            request.getDateTo(),
            request.getBacklog(),
            request.getTimeZone(),
            SIMULATION,
            request.getSimulations(),
            request.isApplyDeviation()
        )
    );

    final List<CptProjectionOutput> actualProjections = getSlaProjectionUseCase.execute(
        new GetSlaProjectionInput(
            workflow,
            request.getWarehouseId(),
            CPT,
            request.getProcessName(),
            request.getDateFrom(),
            request.getDateTo(),
            request.getBacklog(),
            request.getTimeZone(),
            SIMULATION,
            emptyList(),
            request.isApplyDeviation()
        )
    );

    return ResponseEntity.ok(fromProjectionOutputs(simulatedProjections, actualProjections));
  }

  @PostMapping("/deferral/save")
  @Trace(dispatcher = true)
  public ResponseEntity<String> saveSimulations(@PathVariable final Workflow workflow,
                                                @Valid @RequestBody final SaveSimulationRequest request) {

    activateSimulationUseCase.execute(request.toSimulationInput(workflow));

    return ResponseEntity.ok("");
  }


  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(EntityType.class, new EntityTypeEditor());
    dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
  }
}
