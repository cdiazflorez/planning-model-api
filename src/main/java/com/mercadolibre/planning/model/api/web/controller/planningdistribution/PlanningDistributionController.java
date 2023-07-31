package com.mercadolibre.planning.model.api.web.controller.planningdistribution;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.stream.Collectors.groupingBy;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.web.controller.editor.MetricUnitEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows")
public class PlanningDistributionController {

  private final PlanningDistributionService planningDistributionService;

  private final PlannedBacklogService plannedBacklogService;

  @GetMapping("/fbm-wms-outbound/planning_distributions")
  @Trace(dispatcher = true)
  public ResponseEntity<List<GetPlanningDistributionResponse>> getPlanningDist(
      @Valid final GetPlanningDistributionRequest request) {

    final GetPlanningDistributionInput input = request.toGetPlanningDistInput(FBM_WMS_OUTBOUND);

    final Map<PlanningDistributionKey, Double> quantityByPlanningDistributionKey =
        planningDistributionService.getPlanningDistribution(input).stream()
            .collect(
                groupingBy(
                    output -> new PlanningDistributionKey(output.getDateIn(), output.getDateOut(), output.getMetricUnit()),
                    Collectors.summingDouble(GetPlanningDistributionOutput::getTotal)
                )
            );

    return ResponseEntity.status(HttpStatus.OK).body(
        quantityByPlanningDistributionKey.entrySet().stream().map(entry -> new GetPlanningDistributionResponse(
            ofInstant(entry.getKey().getDateIn(), UTC),
            ofInstant(entry.getKey().getDateOut(), UTC),
            entry.getKey().getMetricUnit(),
            Math.round(entry.getValue()),
            false))
            .collect(Collectors.toList()));
  }

  @GetMapping("/fbm-wms-inbound/planning_distributions")
  @Trace(dispatcher = true)
  public ResponseEntity<List<GetPlanningDistributionResponse>> getPlanningDistInbound(
      @Valid final GetPlanningDistributionRequest request) {

    final List<PlannedUnits> plannedUnits = plannedBacklogService.getExpectedBacklog(
        request.getWarehouseId(),
        request.getWorkflow() == null ? FBM_WMS_INBOUND : request.getWorkflow(),
        request.getDateOutFrom(),
        request.getDateOutTo(),
        ofInstant(request.getViewDate(), ZoneId.of("UTC")),
        request.isApplyDeviation()
    );

    return ResponseEntity.status(HttpStatus.OK).body(
        plannedUnits.stream().map(plannedUnit -> new GetPlanningDistributionResponse(plannedUnit.getDateIn(),
                                                                                     plannedUnit.getDateOut(),
                                                                                     UNITS,
                                                                                     plannedUnit.getTotal(),
                                                                                     false))
            .collect(Collectors.toList())
    );
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(MetricUnit.class, new MetricUnitEditor());
  }

  @Value
  private static class PlanningDistributionKey {

    Instant dateIn;

    Instant dateOut;

    MetricUnit metricUnit;

  }
}
