package com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
public class GetPlanningDistributionUseCase
        implements UseCase<GetPlanningDistributionInput, List<GetPlanningDistributionOutput>> {

    private final PlanningDistributionRepository planningDistRepository;

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

    private List<PlanningDistributionView> getPlanningDistributions(
            final GetPlanningDistributionInput input) {

        final ZonedDateTime dateOutFrom = input.getDateOutFrom();
        final ZonedDateTime dateOutTo = input.getDateOutTo();
        final ZonedDateTime dateInTo = input.getDateInTo();

        return dateInTo == null
                ? planningDistRepository
                .findByWarehouseIdWorkflowAndDateOutInRange(
                        input.getWarehouseId(),
                        input.getWorkflow().name(),
                        dateOutFrom,
                        dateOutTo,
                        getForecastWeeks(dateOutFrom, dateOutTo))
                : planningDistRepository
                .findByWarehouseIdWorkflowAndDateOutInRangeAndDateInLessThan(
                        input.getWarehouseId(),
                        input.getWorkflow().name(),
                        dateOutFrom,
                        dateOutTo,
                        dateInTo,
                        getForecastWeeks(dateOutFrom, dateOutTo)
        );

    }
}
