package com.mercadolibre.planning.model.api.web.controller.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.CHECK_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static org.springframework.http.HttpStatus.OK;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.ThroughputService;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessNameEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessPathEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AllArgsConstructor;
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
@RequestMapping("/logistic_center/{logisticCenterId}/plan/staffing/ratio")
public class RatioController {

  private static final Map<Workflow, Set<ProcessName>> PROCESSES_BY_WORKFLOW = Map.of(
      FBM_WMS_OUTBOUND, Set.of(PICKING, PACKING, PACKING_WALL, BATCH_SORTER, WALL_IN),
      FBM_WMS_INBOUND, Set.of(CHECK_IN, PUT_AWAY)
  );

  private final ThroughputService service;

  @GetMapping("/process_path/throughput")
  @Trace(dispatcher = true)
  public ResponseEntity<Map<ProcessPath, Map<ProcessName, Map<Instant, Double>>>> getThroughput(
      @PathVariable final String logisticCenterId,
      @RequestParam final Workflow workflow,
      @RequestParam(required = false) final List<ProcessPath> processPaths,
      @RequestParam(required = false) final Set<ProcessName> processes,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateFrom,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) final Instant dateTo,
      @RequestParam final Instant viewDate
  ) {

    // TODO retrieve all processes if processes params is empty based on processes stored in DB
    final var processesNames = CollectionUtils.isEmpty(processes) ? PROCESSES_BY_WORKFLOW.get(workflow) : processes;

    final var result = service.getThroughputRatioByProcessPath(
        logisticCenterId,
        workflow,
        processPaths,
        processesNames,
        dateFrom,
        dateTo,
        viewDate
    );

    return ResponseEntity.status(OK)
        .body(result);
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(ProcessPath.class, new ProcessPathEditor());
    dataBinder.registerCustomEditor(ProcessName.class, new ProcessNameEditor());
  }
}
