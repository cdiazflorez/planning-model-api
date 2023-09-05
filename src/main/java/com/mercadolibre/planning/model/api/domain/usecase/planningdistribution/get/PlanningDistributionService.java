package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PlanningDistributionService {

  private final GetForecastUseCase getForecastUseCase;

  private final PlanningDistributionGateway planningDistributionGateway;

  private final DeferralGateway deferralGateway;

  private final CurrentForecastDeviationRepository currentForecastDeviationRepository;

  public List<GetPlanningDistributionOutput> getPlanningDistribution(final GetPlanningDistributionInput input) {

    final List<Long> forecastIds = getForecastUseCase.execute(new GetForecastInput(
        input.getWarehouseId(),
        input.getWorkflow(),
        validateDate(input.getDateOutFrom(), input.getDateInFrom()),
        validateDate(input.getDateOutTo(), input.getDateInTo()),
        input.getViewDate()
    ));
    final var nonDeviatedPlanningDistribution = getPlanningDistribution(
        input.getDateOutFrom(),
        input.getDateOutTo(),
        input.getDateInFrom(),
        input.getDateInTo(),
        input.getProcessPaths(),
        forecastIds
    );

    final var deferralDistribution = input.isApplyDeferrals()
        ? applyDeferral(input.getWarehouseId(), input.getWorkflow(), input.getViewDate(), nonDeviatedPlanningDistribution)
        : nonDeviatedPlanningDistribution;

    final var planningDistribution = input.isApplyDeviation()
        ? applyDeviation(input, deferralDistribution)
        : deferralDistribution;

    return planningDistribution.stream()
        .map(pd -> new GetPlanningDistributionOutput(pd.getDateIn(), pd.dateOut, pd.getMetricUnit(), pd.getProcessPath(), pd.getQuantity()))
        .sorted(comparing(GetPlanningDistributionOutput::getDateOut))
        .collect(toList());
  }

  /**
   * Gets the forecasted units.
   * TODO: Avoid calling this method when calculating real forecast deviation card. Instead, create a new endpoint
   * planning_distribution/count to get only units quantity.
   *
   * @param dateOutFrom  slas min date.
   * @param dateOutTo    slas max date.
   * @param dateInFrom   date in from which to return planning distributions.
   * @param dateInTo     date in up to which to return planning distributions.
   * @param processPaths process paths from which to look up planning distributions.
   * @param forecastIds  forecasts ids from which to look up planning distributions.
   * @return planning distributions.
   */
  private List<PlanDistribution> getPlanningDistribution(
      final Instant dateOutFrom,
      final Instant dateOutTo,
      final Instant dateInFrom,
      final Instant dateInTo,
      final Set<ProcessPath> processPaths,
      final List<Long> forecastIds
  ) {
    final List<PlanDistribution> dirtyPlanDistribution = planningDistributionGateway
        .findByForecastIdsAndDynamicFilters(
            dateInFrom,
            dateInTo,
            dateOutFrom,
            dateOutTo,
            processPaths,
            new HashSet<>(forecastIds)
        );

    return removeDuplicatedData(dirtyPlanDistribution);
  }

  /**
   * Gets the applicable deviation corresponding to the specified warehouse and min dateIn of plan distribution.
   */
  private List<PlanDistribution> applyDeviation(
      final GetPlanningDistributionInput input,
      final List<PlanDistribution> planDistribution
  ) {
    final List<CurrentForecastDeviation> activeDeviations;

    final Instant minDateIn = getDateToDeviationQuery(input, planDistribution);

    if (input.getViewDate() == null) {
      activeDeviations = currentForecastDeviationRepository
          .findActiveDeviationAt(input.getWarehouseId(), input.getWorkflow().name(), minDateIn)
          .stream()
          .findFirst().stream().collect(toList());
    } else {
      activeDeviations = currentForecastDeviationRepository.findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThan(
          input.getWarehouseId(),
          input.getWorkflow(),
          ZonedDateTime.ofInstant(minDateIn, ZoneOffset.UTC)
      );
    }

    return activeDeviations.isEmpty()
        ? planDistribution
        : planDistribution.stream()
        .map(distribution -> activeDeviations.stream()
            .filter(deviation -> doesApplyDeviation(deviation.getDateFrom(), distribution.getDateIn(), deviation.getDateTo()))
            .map(deviation -> distribution.newWithAdjustment(deviation.getValue())).findAny().orElse(distribution)
        ).collect(toList());
  }

  private Instant getDateToDeviationQuery(final GetPlanningDistributionInput input,
                                          final List<PlanDistribution> planDistribution) {
    return input.getDateInFrom() == null
        ? planDistribution.stream()
        .min(comparing(distribution -> distribution.dateIn))
        .map(PlanDistribution::getDateIn)
        .orElse(getDefaultDateToSearchDeviation(input.getDateOutFrom(), input.getViewDate()))
        : input.getDateInFrom();
  }

  private Instant getDefaultDateToSearchDeviation(final Instant dateOutFrom, final Instant viewDate) {
    return viewDate == null ? dateOutFrom : viewDate;
  }

  private boolean doesApplyDeviation(final ZonedDateTime lower, final Instant dateIn, final ZonedDateTime higher) {
    return !dateIn.isBefore(lower.toInstant()) && higher.toInstant().isAfter(dateIn);
  }

  private List<PlanDistribution> applyDeferral(
      final String warehouseId,
      final Workflow workflow,
      final Instant viewDate,
      final List<PlanDistribution> planDistribution
  ) {
    final List<Instant> deferredCpts = deferralGateway.getDeferredCpts(warehouseId, workflow, viewDate);

    return planDistribution.stream()
        .filter(plan -> !deferredCpts.contains(plan.getDateOut()))
        .collect(toList());
  }

  /**
   * Removes distribution elements of overlapping forecasts.
   * only the first occurrence is included in the result NOTE that the elements included in the result depend
   * on the order they are in the received list.
   * TODO Eliminar este metodo cuando dejemos guardar el forecast por semana.
   * {@link PlanDistribution#getForecastId()} and the same
   * {@link PlanDistribution#getDateIn()} and
   * {@link PlanDistribution#getDateOut()},
   *
   * @return a list like the received but if two elements have different
   */
  private List<PlanDistribution> removeDuplicatedData(final List<PlanDistribution> duplicatedPlanning) {
    final Map<Pair<Instant, Instant>, Long> dateByForecastId = new ConcurrentHashMap<>();
    final List<PlanDistribution> planning = new ArrayList<>();

    duplicatedPlanning.forEach(elem -> {
      final Pair<Instant, Instant> dateOutDateIn = new ImmutablePair<>(
          elem.getDateOut(), elem.getDateIn()
      );

      final Long winningForecastId = dateByForecastId.get(dateOutDateIn);
      if (winningForecastId == null) {
        dateByForecastId.put(dateOutDateIn, elem.getForecastId());
        planning.add(elem);
      } else if (winningForecastId == elem.getForecastId()) {
        planning.add(elem);
      }
    });
    return planning;
  }

  private Instant validateDate(final Instant dateOut, final Instant dateIn) {
    return dateOut == null ? dateIn : dateOut;
  }

}
