package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentPlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
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
        final Map<Instant, Long> currentQuantities =
                currentPlanningDistRepository
                        .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
                            input.getWorkflow(),
                            input.getWarehouseId(),
                            input.getDateOutFrom(),
                            input.getDateOutTo())
                        .stream()
                        .collect(
                            toMap(d -> d.getDateOut().toInstant(),
                                    CurrentPlanningDistribution::getQuantity));

        final List<PlanningDistributionView> planningDistribution = getPlanningDistributions(input);

        return planningDistribution.stream()
                .map(pd -> GetPlanningDistributionOutput.builder()
                        .metricUnit(UNITS)
                        .dateIn(ofInstant(pd.getDateIn().toInstant(), UTC))
                        .dateOut(ofInstant(pd.getDateOut().toInstant(), UTC))
                        .total(
                                replace(
                                        currentQuantities,
                                        pd.getDateOut().toInstant(),
                                        0L,
                                        pd.getQuantity())
                        )
                        .build())
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

    private Long replace(final Map<Instant, Long> map,
                         final Instant key,
                         final Long value,
                         final Long defaultValue) {
        final Long previous = map.getOrDefault(key, defaultValue);
        map.replace(key, value);
        return previous;
    }
}
