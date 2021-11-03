package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentPlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@AllArgsConstructor
@Service
public class GetPlanningDistributionUseCase
        implements UseCase<GetPlanningDistributionInput, List<GetPlanningDistributionOutput>> {

    private final CurrentPlanningDistributionRepository currentPlanningDistRepository;
    private final PlanningDistributionRepository planningDistRepository;
    private final GetForecastUseCase getForecastUseCase;

    @Override
    public List<GetPlanningDistributionOutput> execute(final GetPlanningDistributionInput input) {
        final Map<Instant, CurrentPlanningDistribution> currentPlanningDistributions =
                currentPlanningDistRepository
                        .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                                input.getWorkflow(),
                                input.getWarehouseId(),
                                input.getDateOutFrom(),
                                input.getDateOutTo())
                        .stream()
                        .collect(toMap(d -> d.getDateOut().toInstant(), Function.identity()));

        final List<PlanningDistributionView> planningDistribution = removeDuplicatedData(
                getPlanningDistributions(input));

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

    // TODO: Avoid calling this method when calculating real forecast deviation card.
    // Instead, create a new endpoint planning_distribution/count to get only units quantity
    private List<PlanningDistributionView> getPlanningDistributions(
            final GetPlanningDistributionInput input) {

        final ZonedDateTime dateOutFrom = input.getDateOutFrom();
        final ZonedDateTime dateOutTo = input.getDateOutTo();
        final ZonedDateTime dateInFrom = input.getDateInFrom();
        final ZonedDateTime dateInTo = input.getDateInTo();

        final List<Long> forecastIds = getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(dateOutFrom)
                .dateTo(dateOutTo)
                .build());

        if (dateInTo == null && dateInFrom == null) {
            return planningDistRepository
                    .findByWarehouseIdWorkflowAndDateOutInRange(
                            input.getWarehouseId(),
                            dateOutFrom,
                            dateOutTo,
                            forecastIds,
                            input.isApplyDeviation());
        } else if (dateInTo != null && dateInFrom == null) {
            return planningDistRepository
                    .findByWarehouseIdWorkflowAndDateOutInRangeAndDateInLessThan(
                            input.getWarehouseId(),
                            dateOutFrom,
                            dateOutTo,
                            dateInTo,
                            forecastIds,
                            input.isApplyDeviation()
                    );
        } else {
            return planningDistRepository
                    .findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
                            input.getWarehouseId(),
                            dateOutFrom,
                            dateOutTo,
                            dateInFrom,
                            dateInTo,
                            forecastIds,
                            input.isApplyDeviation());
        }
    }

    private long getTotal(final Map<Instant, CurrentPlanningDistribution> currentQuantities,
                          final PlanningDistributionView forecastedDistribution) {
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

    // TODO Eliminar este metodo cuando dejemos guardar el forecast por semana
    private List<PlanningDistributionView> removeDuplicatedData(
            final List<PlanningDistributionView> duplicatedPlanning) {

        final Map<Pair<Date, Date>, Long> dateByForecastId = new HashMap<>();
        final List<PlanningDistributionView> planning = new ArrayList<>();

        duplicatedPlanning.forEach(p -> {
            final Pair<Date, Date> dateOutDateIn = new ImmutablePair<>(
                    p.getDateOut(), p.getDateIn());

            final Boolean value =  dateByForecastId.containsKey(dateOutDateIn);
            if (value) {
                if (p.getForecastId() == dateByForecastId.get(dateOutDateIn)) {
                    planning.add(p);
                }
            } else {
                dateByForecastId.put(dateOutDateIn, p.getForecastId());
                planning.add(p);
            }
        });
        return planning;
    }
}
