package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.AMBIENT;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.BULKY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.PP_DEFAULT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.PP_DEFAULT_MULTI;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.REFRIGERATED;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.SIOC;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
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
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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

  private static final Set<ProcessPath> DEFAULT_PROCESS_PATH = Set.of(GLOBAL);

  private static final Map<Workflow, List<ProcessName>> PROCESSES_BY_WORKFLOW = Map.of(
      Workflow.FBM_WMS_OUTBOUND, List.of(PICKING, PACKING, PACKING_WALL, BATCH_SORTER, WALL_IN),
      Workflow.FBM_WMS_INBOUND, List.of(CHECK_IN, PUT_AWAY)
  );

  private static final Map<ProcessPath, Double> RATIOS_BY_PROCESS_PATH;

  static {
    RATIOS_BY_PROCESS_PATH = new EnumMap<>(ProcessPath.class);
    RATIOS_BY_PROCESS_PATH.put(TOT_MONO, 0.25);
    RATIOS_BY_PROCESS_PATH.put(TOT_MULTI_BATCH, 0.1);
    RATIOS_BY_PROCESS_PATH.put(TOT_MULTI_ORDER, 0.13);
    RATIOS_BY_PROCESS_PATH.put(PP_DEFAULT_MONO, 0.09);
    RATIOS_BY_PROCESS_PATH.put(NON_TOT_MONO, 0.03);
    RATIOS_BY_PROCESS_PATH.put(NON_TOT_MULTI_ORDER, 0.11);
    RATIOS_BY_PROCESS_PATH.put(NON_TOT_MULTI_BATCH, 0.18);
    RATIOS_BY_PROCESS_PATH.put(PP_DEFAULT_MULTI, 0.1);
    RATIOS_BY_PROCESS_PATH.put(BULKY, 0.0);
    RATIOS_BY_PROCESS_PATH.put(SIOC, 0.0);
    RATIOS_BY_PROCESS_PATH.put(AMBIENT, 0.005);
    RATIOS_BY_PROCESS_PATH.put(REFRIGERATED, 0.005);
  }

  private final GetThroughputUseCase getThroughputUseCase;

  @GetMapping("/throughput")
  @Trace(dispatcher = true)
  public ResponseEntity<Map<ProcessPath, Map<ProcessName, Map<Instant, Throughput>>>> getThroughput(
      @PathVariable final String logisticCenterId,
      @RequestParam final Workflow workflow,
      @RequestParam(required = false) final Set<ProcessPath> processPaths,
      @RequestParam(required = false) final Set<ProcessName> processes,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateFrom,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateTo,
      @RequestParam final Instant viewDate
  ) {

    // TODO retrieve all processes if processes params is empty based on processes stored in DB
    final var processesNames = isEmpty(processes) ? PROCESSES_BY_WORKFLOW.get(workflow) : new ArrayList<>(processes);

    final GetEntityInput input = GetEntityInput.builder()
        .warehouseId(logisticCenterId)
        .workflow(workflow)
        .dateFrom(ZonedDateTime.ofInstant(dateFrom, ZoneOffset.UTC))
        .dateTo(ZonedDateTime.ofInstant(dateTo, ZoneOffset.UTC))
        .source(SIMULATION)
        .processName(processesNames)
        .simulations(emptyList())
        .viewDate(viewDate)
        .build();

    final var result = getThroughputUseCase.execute(input);
    return ResponseEntity.status(OK).body(adaptResponse(result, processPaths));
  }

  private static Map<ProcessPath, Map<ProcessName, Map<Instant, Throughput>>> adaptResponse(
      final List<EntityOutput> throughputs,
      final Set<ProcessPath> processPaths
  ) {

    final var paths = isEmpty(processPaths) ? DEFAULT_PROCESS_PATH : processPaths;
    return paths.stream()
        .collect(
            Collectors.toMap(
                Function.identity(),
                processPath -> throughputs.stream()
                    .collect(
                        Collectors.groupingBy(
                            EntityOutput::getProcessName,
                            Collectors.toMap(
                                entity -> entity.getDate().toInstant(),
                                entity -> new Throughput(
                                    (int) (entity.getValue() * RATIOS_BY_PROCESS_PATH.get(processPath))
                                ),
                                (a, b) -> new Throughput(a.quantity + b.quantity)
                            )
                        )
                    )
            )
        );
  }

  private static <T> boolean isEmpty(final Collection<T> collection) {
    return collection == null || collection.isEmpty();
  }

  @Value
  static class Throughput {
    long quantity;
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(ProcessPath.class, new ProcessPathEditor());
    dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
  }
}
