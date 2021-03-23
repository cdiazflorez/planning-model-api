package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetCapacityProjectionInput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
public class GetCapacityProjectionUseCase implements
        UseCase<GetCapacityProjectionInput, List<CptProjectionOutput>> {

    private final CalculateCptProjectionUseCase projectionUseCase;

    private final ProcessingDistributionRepository processingDistRepository;

    private final GetForecastUseCase getForecastUseCase;

    @Override
    public List<CptProjectionOutput> execute(final GetCapacityProjectionInput input) {
        final CptProjectionInput projectionInput = CptProjectionInput.builder()
                .capacity(getMaxCapacity(input))
                .backlog(input.getBacklog())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build();

        return projectionUseCase.execute(projectionInput);
    }


    private List<Long> getForecastIds(final GetCapacityProjectionInput input) {
        return getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build()
        );
    }

    private Map<ZonedDateTime, Integer> getMaxCapacity(final GetCapacityProjectionInput input) {

        final List<ProcessingDistributionView> processingDistributionView =
                processingDistRepository
                        .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                                Set.of(ProcessingType.MAX_CAPACITY.name()),
                                List.of(ProcessName.GLOBAL.toJson()),
                                input.getDateFrom(),
                                input.getDateTo(),
                                getForecastIds(input)
                        );
        return processingDistributionView.stream().collect(Collectors.toMap(
                o -> ZonedDateTime.ofInstant(o.getDate().toInstant(), ZoneOffset.UTC),
                o -> (int) o.getQuantity(),
                (intA, intB) -> intB,
                TreeMap::new));
    }
}
