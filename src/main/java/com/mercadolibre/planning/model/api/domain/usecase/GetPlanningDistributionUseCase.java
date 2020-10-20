package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionView;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
public class GetPlanningDistributionUseCase
        implements UseCase<GetPlanningDistributionInput, List<GetPlanningDistributionOutput>> {

    private final PlanningDistributionRepository planningDistRepository;

    @Override
    public List<GetPlanningDistributionOutput> execute(final GetPlanningDistributionInput input) {
        final List<PlanningDistributionView> planningDistributions =
                planningDistRepository.findByWarehouseIdWorkflowAndDateOutInRange(
                        input.getWarehouseId(),
                        input.getWorkflow().name(),
                        input.getDateFrom(),
                        input.getDateTo(),
                        getForecastWeeks(input.getDateFrom(), input.getDateTo()));

        return planningDistributions.stream()
                .map(pd -> GetPlanningDistributionOutput.builder()
                        .metricUnit(UNITS)
                        .dateIn(pd.getDateIn())
                        .dateOut(pd.getDateOut())
                        .total(pd.getQuantity())
                        .build())
                .collect(toList());
    }
}
