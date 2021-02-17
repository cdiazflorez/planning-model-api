package com.mercadolibre.planning.model.api.domain.usecase.entities.productivity.get;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class GetProductivityEntityUseCase
        implements EntityUseCase<GetProductivityInput, List<ProductivityOutput>> {

    protected final HeadcountProductivityRepository productivityRepository;
    protected final CurrentHeadcountProductivityRepository currentProductivityRepository;
    protected final GetForecastUseCase getForecastUseCase;

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == PRODUCTIVITY;
    }

    @Override
    public List<ProductivityOutput> execute(final GetProductivityInput input) {
        if (input.getSource() == FORECAST) {
            return getForecastProductivity(input);
        } else {
            return getSimulationProductivity(input);
        }
    }

    private List<ProductivityOutput> getForecastProductivity(final GetProductivityInput input) {
        final List<HeadcountProductivityView> productivities = findProductivityBy(input);

        return productivities.stream()
                .map(p -> ProductivityOutput.builder()
                        .workflow(input.getWorkflow())
                        .date(ofInstant(p.getDate().toInstant(), UTC))
                        .processName(p.getProcessName())
                        .value(p.getProductivity())
                        .metricUnit(p.getProductivityMetricUnit())
                        .source(FORECAST)
                        .abilityLevel(p.getAbilityLevel())
                        .build())
                .collect(toList());
    }

    private List<ProductivityOutput> getSimulationProductivity(final GetProductivityInput input) {
        final List<CurrentHeadcountProductivity> currentProductivity =
                findCurrentProductivityBy(input);

        final List<ProductivityOutput> entities = getForecastProductivity(input);
        final List<ProductivityOutput> inputSimulatedEntities = createUnappliedSimulations(input);

        currentProductivity.forEach(sp -> {
            if (noSimulationExistsWithSameProperties(inputSimulatedEntities, sp)) {
                entities.add(ProductivityOutput.builder()
                        .workflow(input.getWorkflow())
                        .value(sp.getProductivity())
                        .source(SIMULATION)
                        .processName(sp.getProcessName())
                        .metricUnit(sp.getProductivityMetricUnit())
                        .date(sp.getDate())
                        .abilityLevel(sp.getAbilityLevel())
                        .build());
            }
        });

        entities.addAll(inputSimulatedEntities);
        return new ArrayList<>(entities);
    }

    private boolean noSimulationExistsWithSameProperties(
            final List<ProductivityOutput> entities,
            final CurrentHeadcountProductivity currentHeadcountProductivity) {

        return entities.stream().noneMatch(entityOutput -> entityOutput.getSource() == SIMULATION
                && entityOutput.getProcessName() == currentHeadcountProductivity.getProcessName()
                && entityOutput.getWorkflow() == currentHeadcountProductivity.getWorkflow()
                && entityOutput.getDate().withFixedOffsetZone()
                .isEqual(currentHeadcountProductivity.getDate().withFixedOffsetZone()));
    }

    private List<CurrentHeadcountProductivity> findCurrentProductivityBy(
            final GetProductivityInput input) {

        return currentProductivityRepository
                .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        input.getProcessName(),
                        input.getDateFrom(),
                        input.getDateTo());
    }

    private List<HeadcountProductivityView> findProductivityBy(final GetProductivityInput input) {
        final List<Long> forecastIds = getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build());

        return productivityRepository.findBy(
                input.getProcessNamesAsString(),
                input.getDateFrom(),
                input.getDateTo(),
                forecastIds,
                input.getAbilityLevel());
    }

    private List<ProductivityOutput> createUnappliedSimulations(final GetProductivityInput input) {
        if (input.getSimulations() == null) {
            return Collections.emptyList();
        }
        final List<ProductivityOutput> simulatedEntities = new ArrayList<>();

        input.getSimulations().forEach(simulation ->
                simulation.getEntities().stream()
                        .filter(entity -> entity.getType() == PRODUCTIVITY)
                        .forEach(entity -> {
                            entity.getValues().forEach(quantityByDate ->
                                    simulatedEntities.add(ProductivityOutput.builder()
                                            .workflow(input.getWorkflow())
                                            .date(quantityByDate.getDate().withFixedOffsetZone())
                                            .metricUnit(MetricUnit.UNITS_PER_HOUR)
                                            .processName(simulation.getProcessName())
                                            .source(SIMULATION)
                                            .value(quantityByDate.getQuantity())
                                            .build()));
                        }));
        return simulatedEntities;
    }
}
