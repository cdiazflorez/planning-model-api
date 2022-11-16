package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentPlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PlanningDistributionService {

  private final CurrentPlanningDistributionRepository currentPlanningDistRepository;

  private final PlanningDistributionRepository planningDistRepository;

  private final GetForecastUseCase getForecastUseCase;

  private final CurrentForecastDeviationRepository currentForecastDeviationRepository;

  private static boolean isDateBetween(final ChronoZonedDateTime<LocalDate> lower,
                                       final Date probe,
                                       final ChronoZonedDateTime<LocalDate> higher) {
    final var probeEpochSeconds = probe.toInstant();
    return !probeEpochSeconds.isBefore(lower.toInstant()) && !higher.toInstant().isBefore(probeEpochSeconds);
  }

  public List<GetPlanningDistributionOutput> getPlanningDistribution(final GetPlanningDistributionInput input) {
    final Map<Instant, CurrentPlanningDistribution> currentPlanningDistributions =
        currentPlanningDistRepository
            .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                input.getWorkflow(),
                input.getWarehouseId(),
                input.getDateOutFrom(),
                input.getDateOutTo())
            .stream()
            .collect(toMap(d -> d.getDateOut().toInstant(), Function.identity()));

    final List<Long> forecastIds = getForecastUseCase.execute(new GetForecastInput(
        input.getWarehouseId(),
        input.getWorkflow(),
        input.getDateOutFrom(),
        input.getDateOutTo(),
        input.getViewDate()
    ));
    final var nonDeviatedPlanningDistribution = getPlanningDistribution(
        input.getDateOutFrom(),
        input.getDateOutTo(),
        input.getDateInFrom(),
        input.getDateInTo(),
        forecastIds
    );
    final var planningDistribution = input.isApplyDeviation()
        ? applyDeviation(input.getWarehouseId(), input.getWorkflow(), nonDeviatedPlanningDistribution, input.getViewDate())
        : nonDeviatedPlanningDistribution;

    return planningDistribution.stream()
        .map(pd -> GetPlanningDistributionOutput.builder()
            .metricUnit(UNITS)
            .dateIn(ofInstant(pd.getDateIn().toInstant(), UTC))
            .dateOut(ofInstant(pd.getDateOut().toInstant(), UTC))
            .total(getTotal(currentPlanningDistributions, pd))
            .isDeferred(currentPlanningDistributions.containsKey(
                pd.getDateOut().toInstant())
            )
            .build())
        .sorted(comparing(GetPlanningDistributionOutput::getDateOut))
        .collect(toList());
  }

  /**
   * Gets the forecasted units.
   * TODO: Avoid calling this method when calculating real forecast deviation card. Instead, create a new endpoint
   * planning_distribution/count to get only units quantity.
   *
   * @param dateOutFrom slas min date.
   * @param dateOutTo   slas max date.
   * @param dateInFrom  date in from which to return planning distributions.
   * @param dateInTo    date in up to which to return planning distributions.
   * @param forecastIds forecasts ids from which to look up planning distributions.
   * @return planning distributions.
   */
  public List<PlanningDistributionElemView> getPlanningDistribution(
      final ZonedDateTime dateOutFrom,
      final ZonedDateTime dateOutTo,
      final ZonedDateTime dateInFrom,
      final ZonedDateTime dateInTo,
      final List<Long> forecastIds
  ) {
    final List<PlanningDistributionElemView> dirtyPlanningDistribution = planningDistRepository
        .findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
            dateOutFrom,
            dateOutTo,
            dateInFrom,
            dateInTo,
            forecastIds
        );
    return removeDuplicatedData(dirtyPlanningDistribution);
  }

  /**
   * Applies the newest forecast deviation corresponding of the specified warehouse to the received planning distribution.
   *
   * @param warehouseId          logistic center id from which the deviation should be applied.
   * @param planningDistribution forecasted backlog to be multiplied by the deviation.
   * @return forecasted backlog with deviations applied.
   */
  public List<PlanningDistributionElemView> applyDeviation(
      final String warehouseId,
      final List<PlanningDistributionElemView> planningDistribution
  ) {
    return applyDeviation(warehouseId, FBM_WMS_OUTBOUND, planningDistribution, null);
  }

  private List<PlanningDistributionElemView> applyDeviation(
      final String warehouseId,
      final Workflow workflow,
      final List<PlanningDistributionElemView> planningDistribution,
      final Instant viewDate
  ) {
    return getApplicableForecastDeviation(warehouseId, workflow, viewDate)
        .map(deviation -> planningDistribution.stream()
            .map(elem ->
                isDateBetween(
                    deviation.getDateFrom(),
                    elem.getDateIn(),
                    deviation.getDateTo()
                )
                    ? PlanningDistributionViewImpl.fromWithQuantity(
                    elem,
                    Math.round(elem.getQuantity() * (1.0 + deviation.getValue()))
                )
                    : elem
            )
            .collect(toList()))
        .orElse(planningDistribution);
  }

  /**
   * Gets the applicable deviation corresponding to the specified warehouse.
   */
  private Optional<CurrentForecastDeviation> getApplicableForecastDeviation(
      final String warehouseId,
      final Workflow workflow,
      final Instant viewDate
  ) {
    return viewDate == null
        ? currentForecastDeviationRepository.findByLogisticCenterIdAndWorkflowAndIsActiveTrue(warehouseId, workflow)
        .stream()
        .max(comparing(CurrentForecastDeviation::getLastUpdated))
        : currentForecastDeviationRepository.findActiveDeviationAt(warehouseId, workflow.name(), viewDate)
        .stream()
        .findFirst();
  }

  private long getTotal(final Map<Instant, CurrentPlanningDistribution> currentQuantities,
                        final PlanningDistributionElemView forecastedDistribution) {
    final CurrentPlanningDistribution current =
        currentQuantities.get(forecastedDistribution.getDateOut().toInstant());

    if (current == null
        || forecastedDistribution.getDateIn().toInstant()
        .isBefore(current.getDateInFrom().toInstant())) {
      return forecastedDistribution.getQuantity();
    }

    return replace(
        currentQuantities,
        forecastedDistribution.getDateOut().toInstant(),
        0L,
        current
    );
  }

  private Long replace(final Map<Instant, CurrentPlanningDistribution> map,
                       final Instant key,
                       final Long value,
                       final CurrentPlanningDistribution current) {
    final long previous = current.getQuantity();
    current.setQuantity(value);
    map.replace(key, current);
    return previous;
  }


  /**
   * Removes distribution elements of overlapping forecasts.
   * only the first occurrence is included in the result NOTE that the elements included in the result depend
   * on the order they are in the received list.
   * TODO Eliminar este metodo cuando dejemos guardar el forecast por semana.
   *
   * {@link PlanningDistributionElemView#getForecastId()} and the same
   * {@link PlanningDistributionElemView#getDateIn()} and
   * {@link PlanningDistributionElemView#getDateOut()},
   *
   * @return a list like the received but if two elements have different
   */
  private List<PlanningDistributionElemView> removeDuplicatedData(final List<PlanningDistributionElemView> duplicatedPlanning) {
    final Map<Pair<Date, Date>, Long> dateByForecastId = new HashMap<>();
    final List<PlanningDistributionElemView> planning = new ArrayList<>();

    duplicatedPlanning.forEach(elem -> {
      final Pair<Date, Date> dateOutDateIn = new ImmutablePair<>(
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
}
