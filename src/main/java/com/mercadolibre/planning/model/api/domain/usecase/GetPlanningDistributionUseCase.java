package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
public class GetPlanningDistributionUseCase {

    private final PlanningDistributionRepository planningDistRepository;

    public List<GetPlanningDistributionOutput> execute(final GetPlanningDistributionInput input) {
        final List<PlanningDistribution> planningDistributions =
                planningDistRepository.findByWarehouseIdWorkflowAndDateOutInRange(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        input.getDateFrom(),
                        input.getDateTo());

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
