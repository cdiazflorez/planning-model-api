package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get.ProductivityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.ActivateSimulationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.simulation.activate.SimulationInput;
import com.mercadolibre.planning.model.api.util.StaffingPlanMapper;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessPathEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.plan.staffing.request.UpdateStaffingPlanRequest;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/logistic_center/{logisticCenterId}/plan/staffing")
public class Controller {

  private static final Map<Workflow, List<ProcessName>> PROCESSES_BY_WORKFLOW = Map.of(
      Workflow.FBM_WMS_OUTBOUND, List.of(PICKING, PACKING, PACKING_WALL, BATCH_SORTER, WALL_IN),
      Workflow.FBM_WMS_INBOUND, List.of(CHECK_IN, PUT_AWAY)
  );

  private final GetThroughputUseCase getThroughputUseCase;

  private final GetHeadcountEntityUseCase getHeadcountEntityUseCase;

  private final GetProductivityEntityUseCase getProductivityEntityUseCase;

  private final ActivateSimulationUseCase activateSimulationUseCase;

  @GetMapping
  @Trace(dispatcher = true)
  public ResponseEntity<StaffingPlanMapper.StaffingPlan> getStaffingPlan(
      @PathVariable final String logisticCenterId,
      @RequestParam final Workflow workflow,
      @RequestParam(required = false) final List<ProcessPath> processPaths,
      @RequestParam(required = false) final Set<ProcessName> processes,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateFrom,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateTo,
      @RequestParam final Instant viewDate
  ) {

    final GetEntityInput input = createEntityInput(
        logisticCenterId,
        workflow,
        processPaths,
        processes,
        dateFrom,
        dateTo,
        viewDate
    );
    final List<EntityOutput> headcounts = getHeadcountEntityUseCase.execute(StaffingPlanMapper.createSystemicHeadcountInput(input));
    final List<EntityOutput> headcountsNs = getHeadcountEntityUseCase.execute(StaffingPlanMapper.createNonSystemicHeadcountInput(input));
    final List<ProductivityOutput> productivity = getProductivityEntityUseCase.execute(StaffingPlanMapper.createProductivityInput(input));
    final List<EntityOutput> throughput = getThroughputUseCase.execute(input);

    return ResponseEntity.status(OK).body(new StaffingPlanMapper.StaffingPlan(
        StaffingPlanMapper.adaptEntityOutputResponse(headcounts),
        StaffingPlanMapper.adaptEntityOutputResponse(headcountsNs),
        StaffingPlanMapper.adaptEntityOutputResponse(
            productivity.stream().filter(ProductivityOutput::isMainProductivity).collect(toList())
        ),
        StaffingPlanMapper.adaptThroughputResponse(throughput)
    ));
  }

  @ResponseStatus(OK)
  @PutMapping
  @Trace(dispatcher = true)
  public void updateStaffingPlan(@PathVariable final String logisticCenterId,
                                 @RequestParam final Workflow workflow,
                                 @RequestParam final long userId,
                                 @RequestBody final UpdateStaffingPlanRequest request) {

    final SimulationInput input = request.toSimulationInput(workflow, logisticCenterId, userId);
    activateSimulationUseCase.execute(input);
  }

  @GetMapping("/throughput")
  @Trace(dispatcher = true)
  public ResponseEntity<Map<ProcessPath, Map<ProcessName, Map<Instant, StaffingPlanMapper.StaffingPlanThroughput>>>> getThroughput(
      @PathVariable final String logisticCenterId,
      @RequestParam final Workflow workflow,
      @RequestParam(required = false) final List<ProcessPath> processPaths,
      @RequestParam(required = false) final Set<ProcessName> processes,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateFrom,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateTo,
      @RequestParam final Instant viewDate
  ) {
    final GetEntityInput input = createEntityInput(
        logisticCenterId,
        workflow,
        processPaths,
        processes,
        dateFrom,
        dateTo,
        viewDate
    );
    final var result = getThroughputUseCase.execute(input);
    return ResponseEntity.status(OK).body(StaffingPlanMapper.adaptThroughputResponse(result));
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(ProcessPath.class, new ProcessPathEditor());
    dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
  }

  private GetEntityInput createEntityInput(
      final String logisticCenterId,
      final Workflow workflow,
      final List<ProcessPath> processPaths,
      final Set<ProcessName> processes,
      final Instant dateFrom,
      final Instant dateTo,
      final Instant viewDate
  ) {

    // TODO retrieve all processes if processes params is empty based on processes stored in DB
    final var processesNames = CollectionUtils.isEmpty(processes)
        ? PROCESSES_BY_WORKFLOW.get(workflow)
        : new ArrayList<>(processes);

    return GetEntityInput.builder()
        .warehouseId(logisticCenterId)
        .workflow(workflow)
        .dateFrom(ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC))
        .dateTo(ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC))
        .source(SIMULATION)
        .processName(processesNames)
        .processPaths(processPaths)
        .simulations(emptyList())
        .viewDate(viewDate)
        .build();
  }
}
