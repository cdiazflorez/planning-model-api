package com.mercadolibre.planning.model.api.web.controller.projection;

import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.BacklogProjectionUseCaseFactory;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.GetBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.BacklogProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.BacklogProjection;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.QueueProjectionService;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeferralProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeliveryPromiseProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetSlaProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeferralProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetSlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.output.DeferralProjectionOutput;
import com.mercadolibre.planning.model.api.web.controller.editor.ProjectionTypeEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import com.mercadolibre.planning.model.api.web.controller.projection.request.BacklogProjectionByAreaRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.BacklogProjectionRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.CptProjectionRequest;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.projection.response.BacklogProjectionByAreaDto;
import com.newrelic.api.agent.Trace;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LongVariable"})
@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/workflows/{workflow}/projections")
@Slf4j
public class ProjectionController {

  private static final String COMMAND_CENTER_SLA = "COMMAND_CENTER_SLA";

  private static final long CAP5_LIMIT_HOURS = 72;

  private final GetDeliveryPromiseProjectionUseCase delPromiseProjection;

  private final GetDeferralProjectionUseCase deferralProjectionUseCase;

  private final GetSlaProjectionUseCase getSlaProjectionUseCase;

  private final BacklogProjectionUseCaseFactory backlogProjectionUseCaseFactory;

  private final BacklogProjectionAdapter backlogProjectionAdapter;

  private final QueueProjectionService queueProjectionService;

  @PostMapping("/cpts")
  @Trace(dispatcher = true)
  public ResponseEntity<List<CptProjectionOutput>> getCptProjection(
      @PathVariable final Workflow workflow,
      @Valid @RequestBody final CptProjectionRequest request) {

    var input = new GetSlaProjectionInput(
        workflow,
        request.getWarehouseId(),
        request.getType(),
        request.getProcessName(),
        request.getDateFrom(),
        request.getDateTo(),
        request.getBacklog(),
        request.getTimeZone(),
        SIMULATION,
        emptyList(),
        request.isApplyDeviation()
    );
    if (request.getType() != null && COMMAND_CENTER_SLA.equals(request.getType().name())) {
      return ResponseEntity.ok(queueProjectionService.calculateCptProjection(input));
    } else {
      return ResponseEntity.ok(getSlaProjectionUseCase.execute(input));
    }
  }

  @PostMapping("/cpts/delivery_promise")
  @Trace(dispatcher = true)
  public ResponseEntity<List<DeliveryPromiseProjectionOutput>> getDeliveryPromiseProjection(
      @PathVariable final Workflow workflow,
      @Valid @RequestBody final CptProjectionRequest request) {

    return ResponseEntity.ok(delPromiseProjection.execute(GetDeliveryPromiseProjectionInput
        .builder()
        .warehouseId(request.getWarehouseId())
        .workflow(workflow)
        .projectionType(request.getType())
        .dateFrom(request.getDateFrom())
        .dateTo(request.getDateTo())
        .timeZone(request.getTimeZone())
        .backlog(getBacklog(request.getBacklog()))
        .applyDeviation(request.isApplyDeviation())
        .simulations(request.getSimulations())
        .build())
    );
  }

  @PostMapping("/cpts/deferral_time")
  @Trace(dispatcher = true)
  public ResponseEntity<List<DeferralProjectionOutput>> getDeferralProjection(@PathVariable final Workflow workflow,
                                                                              @Valid @RequestBody final CptProjectionRequest request) {

    final var backlog = request.getBacklog()
        .stream()
        .map(quantityAtDate -> new Backlog(quantityAtDate.getDate(), quantityAtDate.getQuantity()))
        .collect(toList());

    final var dateFrom = request.getDateFrom().truncatedTo(ChronoUnit.HOURS);
    final var slaTo = request.getDateTo().plus(CAP5_LIMIT_HOURS, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);

    return ResponseEntity.ok(
        deferralProjectionUseCase.execute(
            new GetDeferralProjectionInput(
                request.getWarehouseId(),
                workflow,
                request.getType(),
                request.getDateFrom().toInstant(),
                dateFrom,
                request.getDateTo(),
                dateFrom,
                slaTo,
                backlog,
                request.getTimeZone(),
                request.isApplyDeviation(),
                request.getSimulations()
            )
        )
    );
  }

  @PostMapping("/backlogs")
  @Trace(dispatcher = true)
  public ResponseEntity<List<BacklogProjection>> getBacklogProjections(
      @PathVariable final Workflow workflow,
      @Valid @RequestBody final BacklogProjectionRequest request) {

    final GetBacklogProjectionUseCase useCase = backlogProjectionUseCaseFactory.getUseCase(workflow);

    return ResponseEntity.ok(useCase.execute(BacklogProjectionInput.builder()
        .logisticCenterId(request.getWarehouseId())
        .dateFrom(request.getDateFrom())
        .dateTo(request.getDateTo())
        .currentBacklogs(request.getCurrentBacklog())
        .processNames(request.getProcessName())
        .ratioPackingRegular(request.getRatioPackingRegular())
        .build()));
  }

  @PostMapping("/backlogs/grouped/area")
  @Trace(dispatcher = true)
  public ResponseEntity<List<BacklogProjectionByAreaDto>> getBacklogByAreaProjections(
      @PathVariable final Workflow workflow,
      @Valid @RequestBody final BacklogProjectionByAreaRequest request) {

    final var projections = backlogProjectionAdapter.projectionByArea(
        request.getDateFrom(),
        request.getDateTo(),
        workflow,
        request.getProcessName(),
        request.getThroughput(),
        request.getPlanningUnits(),
        request.getCurrentBacklog(),
        request.getAreaDistributions()
    );

    return ResponseEntity.ok(projections);
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(ProjectionType.class, new ProjectionTypeEditor());
  }

  private List<Backlog> getBacklog(final List<QuantityByDate> backlogs) {
    return backlogs == null
        ? emptyList()
        : backlogs.stream().map(QuantityByDate::toBacklog).collect(toList());
  }
}
