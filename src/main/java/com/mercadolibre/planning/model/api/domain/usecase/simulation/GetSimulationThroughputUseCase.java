package com.mercadolibre.planning.model.api.domain.usecase.simulation;

import com.mercadolibre.planning.model.api.domain.usecase.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetProductivityEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.GetThroughputEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class GetSimulationThroughputUseCase extends GetThroughputEntityUseCase {

    private final GetProductivityEntityUseCase getProductivityEntityUseCase;
    private final GetHeadcountEntityUseCase getHeadcountEntityUseCase;

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        return createThroughputs(getMergedHeadcount(input), getMergedProductivity(input));
    }

    private List<EntityOutput> getMergedHeadcount(final GetEntityInput input) {
        final List<EntityOutput> forecastedHeadcount = getForecastHeadcount(input);
        return getDefinitiveEntities(input, forecastedHeadcount, HEADCOUNT);
    }

    private List<EntityOutput> getMergedProductivity(final GetEntityInput input) {
        final List<EntityOutput> forecastedProductivity = getForecastProductivity(input);
        return getDefinitiveEntities(input, forecastedProductivity, PRODUCTIVITY);
    }

    private List<EntityOutput> getForecastHeadcount(final GetEntityInput input) {
        final GetEntityInput forecastHeadcount = new GetEntityInput(
                input.getWarehouseId(),
                input.getWorkflow(),
                HEADCOUNT,
                input.getDateFrom(),
                input.getDateTo(),
                FORECAST,
                input.getProcessName(),
                input.getProcessingType(),
                null
        );

        return getHeadcountEntityUseCase.execute(forecastHeadcount);
    }

    private List<EntityOutput> getForecastProductivity(final GetEntityInput input) {
        final GetEntityInput forecastProductivity = new GetEntityInput(
                input.getWarehouseId(),
                input.getWorkflow(),
                PRODUCTIVITY,
                input.getDateFrom(),
                input.getDateTo(),
                FORECAST,
                input.getProcessName(),
                input.getProcessingType(),
                null
        );

        return getProductivityEntityUseCase.execute(forecastProductivity);
    }

    private List<EntityOutput> getDefinitiveEntities(final GetEntityInput input,
                                                     final List<EntityOutput> forecastedEntities,
                                                     final EntityType entityType) {

        final List<EntityOutput> simulatedEntities = input.getSimulations().stream()
                .map(s -> s.toEntityOutputs(input.getWorkflow(), entityType))
                .flatMap(List::stream)
                .collect(toList());

        forecastedEntities.stream()
                .filter(fh -> simulationDoesNotExist(fh, simulatedEntities))
                .forEach(simulatedEntities::add);

        return simulatedEntities;
    }

    private boolean simulationDoesNotExist(final EntityOutput forecastEntity,
                                           final List<EntityOutput> simulatedEntities) {
        return simulatedEntities.stream()
                .noneMatch(simulatedEntity ->
                        simulatedEntity.getProcessName() == forecastEntity.getProcessName()
                                && simulatedEntity.getWorkflow() == forecastEntity.getWorkflow()
                                && simulatedEntity.getDate().isEqual(forecastEntity.getDate()));
    }
}
