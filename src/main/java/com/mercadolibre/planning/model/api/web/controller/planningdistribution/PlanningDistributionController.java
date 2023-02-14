package com.mercadolibre.planning.model.api.web.controller.planningdistribution;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;

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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
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
  public ResponseEntity<List<GetPlanningDistributionOutput>> getPlanningDist(
      @Valid final GetPlanningDistributionRequest request) {

    final GetPlanningDistributionInput input = request.toGetPlanningDistInput(FBM_WMS_OUTBOUND);
    return ResponseEntity.status(HttpStatus.OK).body(planningDistributionService.getPlanningDistribution(input));
  }

  @GetMapping("/fbm-wms-inbound/planning_distributions")
  @Trace(dispatcher = true)
  public ResponseEntity<List<GetPlanningDistributionOutput>> getPlanningDistInbound(
      @Valid final GetPlanningDistributionRequest request) {

    final List<PlannedUnits> plannedUnits = plannedBacklogService.getExpectedBacklog(
        request.getWarehouseId(),
        request.getWorkflow() == null ? FBM_WMS_INBOUND : request.getWorkflow(),
        request.getDateOutFrom(),
        request.getDateOutTo(),
        ZonedDateTime.ofInstant(request.getViewDate(), ZoneId.of("UTC")),
        request.isApplyDeviation()
    );

    return ResponseEntity.status(HttpStatus.OK).body(
        plannedUnits.stream().map(plannedUnit -> GetPlanningDistributionOutput.builder()
            .dateIn(plannedUnit.getDateIn())
            .dateOut(plannedUnit.getDateOut())
            .metricUnit(UNITS)
            .total(plannedUnit.getTotal())
            .build()
        ).collect(Collectors.toList())
    );
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(MetricUnit.class, new MetricUnitEditor());
  }
}
