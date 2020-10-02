package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.output.HeadcountOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
public class GetHeadcountEntityUseCase implements GetEntityUseCase {

    private final ProcessingDistributionRepository processingDistRepository;

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        if (input.getSource() == null || input.getSource() == FORECAST) {
            return getForecastHeadcount(input);
        } else {
            return getSimulationHeadcount();
        }
    }

    private List<EntityOutput> getForecastHeadcount(final GetEntityInput input) {
        final List<ProcessingDistribution> processingDistributions = processingDistRepository
                .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        ProcessingType.ACTIVE_WORKERS,
                        input.getProcessName(),
                        input.getDateFrom(),
                        input.getDateTo());

        return processingDistributions.stream()
                .map(p -> HeadcountOutput.builder()
                        .workflow(input.getWorkflow())
                        .date(p.getDate())
                        .processName(p.getProcessName())
                        .value(p.getQuantity())
                        .metricUnit(p.getQuantityMetricUnit())
                        .source(FORECAST)
                        .build())
                .collect(toList());
    }

    private List<EntityOutput> getSimulationHeadcount() {
        //TODO: Add SIMULATION logic
        return emptyList();
    }

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == HEADCOUNT;
    }
}
