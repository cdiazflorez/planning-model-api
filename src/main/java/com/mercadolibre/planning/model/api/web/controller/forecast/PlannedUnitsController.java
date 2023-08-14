package com.mercadolibre.planning.model.api.web.controller.forecast;

import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper.DATE_IN;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper.DATE_OUT;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.exception.InvalidDateRangeException;
import com.mercadolibre.planning.model.api.web.controller.editor.GrouperEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.ProcessPathEditor;
import com.mercadolibre.planning.model.api.web.controller.editor.WorkflowEditor;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.beans.PropertyEditorRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/logistic_center/{logisticCenter}/plan/units")
public class PlannedUnitsController {

  private PlanningDistributionService planningDistributionService;

  @GetMapping
  public ResponseEntity<List<PlanningDistributionOutput>> getForecast(
      @PathVariable final String logisticCenter,
      @RequestParam final Workflow workflow,
      @RequestParam(required = false) final Set<ProcessPath> processPaths,
      @RequestParam(required = false) final Instant dateInFrom,
      @RequestParam(required = false) final Instant dateInTo,
      @RequestParam(required = false) final Instant dateOutFrom,
      @RequestParam(required = false) final Instant dateOutTo,
      @RequestParam final Instant viewDate,
      @RequestParam final Set<Grouper> groupBy,
      @RequestParam(required = false) final boolean excludeDeferred,
      @RequestParam(required = false) final boolean applyDeferrals
  ) {

    validateDatesRanges(dateInFrom, dateInTo, dateOutFrom, dateOutTo);

    final var quantityByGroupKey = planningDistributionService.getPlanningDistribution(
            GetPlanningDistributionInput.builder()
                .warehouseId(logisticCenter)
                .workflow(workflow)
                .dateInFrom(dateInFrom)
                .dateInTo(dateInTo)
                .dateOutFrom(dateOutFrom)
                .dateOutTo(dateOutTo)
                .processPaths(processPaths)
                .applyDeviation(true)
                .viewDate(viewDate)
                .applyDeferrals(applyDeferrals || excludeDeferred)
                .build()).stream()
        .collect(
            toMap(
                planningDistributionOutput -> buildGroupKey(planningDistributionOutput, groupBy),
                GetPlanningDistributionOutput::getTotal,
                Double::sum)
        );

    return ResponseEntity.status(HttpStatus.OK).body(
        quantityByGroupKey.entrySet().stream()
            .map(entry -> new PlanningDistributionOutput(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList())
    );
  }

  private PlanningDistributionOutput.GroupKey buildGroupKey(final GetPlanningDistributionOutput planningDistributionOutput,
                                                            final Set<Grouper> groupers) {
    final var grouperKeyBuilder = PlanningDistributionOutput.GroupKey.builder();
    groupers.forEach(grouper -> {
      if (grouper == DATE_IN) {
        grouperKeyBuilder.dateIn(planningDistributionOutput.getDateIn());
      } else if (grouper == DATE_OUT) {
        grouperKeyBuilder.dateOut(planningDistributionOutput.getDateOut());
      } else {
        grouperKeyBuilder.processPath(planningDistributionOutput.getProcessPath());
      }
    });
    return grouperKeyBuilder.build();
  }

  private void validateDatesRanges(final Instant dateInFrom,
                                   final Instant dateInTo,
                                   final Instant dateOutFrom,
                                   final Instant dateOutTo) {
    if (!((dateInFrom != null && dateInTo != null) || (dateOutFrom != null && dateOutTo != null))) {
      throw new InvalidDateRangeException(dateInFrom, dateInTo, dateOutFrom, dateOutTo);
    }
  }

  @InitBinder
  public void initBinder(final PropertyEditorRegistry dataBinder) {
    dataBinder.registerCustomEditor(Workflow.class, new WorkflowEditor());
    dataBinder.registerCustomEditor(ProcessPath.class, new ProcessPathEditor());
    dataBinder.registerCustomEditor(Grouper.class, new GrouperEditor());
  }

}
