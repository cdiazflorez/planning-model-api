package com.mercadolibre.planning.model.api.domain.usecase.entities.remainingprocessing.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.REMAINING_PROCESSING;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
@SuppressWarnings({"PMD.ExcessiveImports"})
public class GetRemainingProcessingUseCase
        implements EntityUseCase<GetEntityInput, List<EntityOutput>> {

    private final ProcessingDistributionRepository processingDistRepository;
    private final GetForecastUseCase getForecastUseCase;

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == EntityType.REMAINING_PROCESSING;
    }

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        final List<Long> forecastIds = getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build());

        final List<ProcessingDistributionView> remainingProcessing = processingDistRepository
                .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        Set.of(REMAINING_PROCESSING.name()),
                        input.getProcessNamesAsString(),
                        input.getDateFrom(),
                        input.getDateTo(),
                        forecastIds
                );

        return remainingProcessing.stream()
                .map(rp -> EntityOutput.fromProcessingDistributionView(rp, input.getWorkflow()))
                .collect(toList());
    }

}
