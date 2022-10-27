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
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessPathEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/flow/logistic_center/{logisticCenterId}/plan/staffing")
public class Controller {

  private static final Map<Workflow, List<ProcessName>> PROCESSES_BY_WORKFLOW = Map.of(
      Workflow.FBM_WMS_OUTBOUND, List.of(PICKING, PACKING, PACKING_WALL, BATCH_SORTER, WALL_IN),
      Workflow.FBM_WMS_INBOUND, List.of(CHECK_IN, PUT_AWAY)
  );

  private final GetThroughputUseCase getThroughputUseCase;

  private static Map<ProcessPath, Map<ProcessName, Map<Instant, Throughput>>> adaptResponse(final List<EntityOutput> throughputs) {
    return throughputs.stream().collect(
        Collectors.groupingBy(EntityOutput::getProcessPath,
            Collectors.groupingBy(EntityOutput::getProcessName,
                Collectors.toMap(entity -> entity.getDate().toInstant(),
                    entity -> new Throughput(entity.getValue())
                )
            )
        )
    );
  }

  @GetMapping("/throughput")
  @Trace(dispatcher = true)
  public ResponseEntity<Map<ProcessPath, Map<ProcessName, Map<Instant, Throughput>>>> getThroughput(
      @PathVariable final String logisticCenterId,
      @RequestParam final Workflow workflow,
      @RequestParam(required = false) final List<ProcessPath> processPaths,
      @RequestParam(required = false) final Set<ProcessName> processes,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateFrom,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateTo,
      @RequestParam final Instant viewDate
  ) {

    // TODO retrieve all processes if processes params is empty based on processes stored in DB
    final var processesNames = CollectionUtils.isEmpty(processes) ? PROCESSES_BY_WORKFLOW.get(workflow) : new ArrayList<>(processes);

    final GetEntityInput input = GetEntityInput.builder()
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

    final var result = getThroughputUseCase.execute(input);
    return ResponseEntity.status(OK).body(adaptResponse(result));
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(ProcessPath.class, new ProcessPathEditor());
    dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
  }

  @Value
  static class Throughput {
    long quantity;
  }
}
