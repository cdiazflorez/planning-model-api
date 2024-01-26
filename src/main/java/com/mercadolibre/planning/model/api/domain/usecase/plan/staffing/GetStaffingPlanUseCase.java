package com.mercadolibre.planning.model.api.domain.usecase.plan.staffing;

import static java.time.ZoneOffset.UTC;

import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.plan.CurrentStaffingPlanInput;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlan;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlanInput;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlanResponse;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.gateway.CurrentProcessingDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetStaffingPlanUseCase {

  private final GetForecastUseCase getForecastUseCase;

  private final ProcessingDistributionGateway processingDistributionGateway;

  private final CurrentProcessingDistributionGateway currentProcessingDistributionGateway;

  public List<StaffingPlanResponse> execute(final String logisticCenterId,
                                            final Workflow workflow,
                                            final ProcessingType type,
                                            final List<String> groupers,
                                            final Map<String, List<String>> filters,
                                            final Instant dateFrom,
                                            final Instant dateTo,
                                            final Instant viewDate) {

    final List<Long> forecastIds = getForecastUseCase.execute(GetForecastInput.builder()
                                                                  .dateFrom(ZonedDateTime.ofInstant(dateFrom, UTC))
                                                                  .dateTo(ZonedDateTime.ofInstant(dateTo, UTC))
                                                                  .viewDate(viewDate)
                                                                  .warehouseId(logisticCenterId)
                                                                  .workflow(workflow)
                                                                  .build()
    );

    final List<StaffingPlan> staffingPlans = processingDistributionGateway.getStaffingPlan(
        new StaffingPlanInput(forecastIds, dateFrom, dateTo, type, groupers, filters)
    );

    final List<StaffingPlan> currentStaffingPlans = currentProcessingDistributionGateway.getCurrentStaffingPlan(
        new CurrentStaffingPlanInput(logisticCenterId, workflow, dateFrom, dateTo, type, groupers, filters)
    );

    return mergeStaffingPlanToResource(staffingPlans, currentStaffingPlans);
  }

  private List<StaffingPlanResponse> mergeStaffingPlanToResource(final List<StaffingPlan> staffingPlans,
                                                                 final List<StaffingPlan> currentStaffingPlans) {
    return staffingPlans.stream()
        .map(staffingPlan -> new StaffingPlanResponse(
            getCurrentStaffingPlanQuantity(staffingPlan, currentStaffingPlans),
            staffingPlan.quantity(),
            staffingPlan.grouper()
        ))
        .toList();
  }

  private double getCurrentStaffingPlanQuantity(final StaffingPlan staffingPlan, final List<StaffingPlan> currentStaffingPlans) {
    return currentStaffingPlans.stream()
        .filter(currentStaffingPlan -> currentStaffingPlan.isEqualsWithoutQuantity(staffingPlan))
        .findFirst()
        .map(StaffingPlan::quantity)
        .orElse(staffingPlan.quantity());
  }

}
