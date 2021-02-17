package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
public class GetPlanningDistributionUseCase
        implements UseCase<GetPlanningDistributionInput, List<GetPlanningDistributionOutput>> {

    private final PlanningDistributionRepository planningDistRepository;
    private final GetForecastUseCase getForecastUseCase;

    @Override
    public List<GetPlanningDistributionOutput> execute(final GetPlanningDistributionInput input) {
        final List<PlanningDistributionView> planningDistribution = getPlanningDistributions(input);

        return planningDistribution.stream()
                .map(pd -> GetPlanningDistributionOutput.builder()
                        .metricUnit(UNITS)
                        .dateIn(ofInstant(pd.getDateIn().toInstant(), UTC))
                        .dateOut(ofInstant(pd.getDateOut().toInstant(), UTC))
                        .total(pd.getQuantity())
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
}
