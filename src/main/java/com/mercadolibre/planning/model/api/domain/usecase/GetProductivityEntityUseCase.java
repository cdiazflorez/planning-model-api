package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentHeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.HeadcountProductivityView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class GetProductivityEntityUseCase implements GetEntityUseCase {

    protected final HeadcountProductivityRepository productivityRepository;
    protected final CurrentHeadcountProductivityRepository currentProductivityRepository;

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        if (input.getSource() == FORECAST) {
            return getForecastProductivity(input);
        } else {
            return getSimulationProductivity(input);
        }
    }

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == PRODUCTIVITY;
    }

    private List<EntityOutput> getForecastProductivity(final GetEntityInput input) {
        final List<HeadcountProductivityView> productivities = findProductivityBy(input);

        return productivities.stream()
                .map(p -> EntityOutput.builder()
                        .workflow(input.getWorkflow())
                        .date(ofInstant(p.getDate().toInstant(), UTC))
                        .processName(p.getProcessName())
                        .value(p.getProductivity())
                        .metricUnit(p.getProductivityMetricUnit())
                        .source(FORECAST)
                        .build())
                .collect(toList());
    }

    private List<EntityOutput> getSimulationProductivity(final GetEntityInput input) {
        final List<CurrentHeadcountProductivity> currentProductivity =
                findCurrentProductivityBy(input);

        final List<EntityOutput> entities = getForecastProductivity(input);
        final List<EntityOutput> inputSimulatedEntities = createUnappliedSimulations(input);

        currentProductivity.forEach(sp -> {
            if (noSimulationExistsWithSameProperties(inputSimulatedEntities, sp)) {
                entities.add(EntityOutput.builder()
                        .workflow(input.getWorkflow())
                        .value(sp.getProductivity())
                        .source(SIMULATION)
                        .processName(sp.getProcessName())
                        .metricUnit(sp.getProductivityMetricUnit())
                        .date(sp.getDate())
                        .build());
            }
        });

        entities.addAll(inputSimulatedEntities);
        return new ArrayList<>(entities);
    }

    private boolean noSimulationExistsWithSameProperties(
            final List<EntityOutput> entities,
            final CurrentHeadcountProductivity currentHeadcountProductivity) {

        return entities.stream().noneMatch(entityOutput -> entityOutput.getSource() == SIMULATION
                && entityOutput.getProcessName() == currentHeadcountProductivity.getProcessName()
                && entityOutput.getWorkflow() == currentHeadcountProductivity.getWorkflow()
                && entityOutput.getDate().withFixedOffsetZone()
                .isEqual(currentHeadcountProductivity.getDate().withFixedOffsetZone()));
    }

    private List<CurrentHeadcountProductivity> findCurrentProductivityBy(
            final GetEntityInput input) {

        return currentProductivityRepository
                .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        input.getProcessName(),
                        input.getDateFrom(),
                        input.getDateTo());
    }

    private List<HeadcountProductivityView> findProductivityBy(final GetEntityInput input) {
        return productivityRepository.findBy(
                input.getWarehouseId(),
                input.getWorkflow().name(),
                input.getProcessName().stream().map(Enum::name).collect(toList()),
                input.getDateFrom(),
                input.getDateTo(),
                getForecastWeeks(input.getDateFrom(), input.getDateTo()),
                input.getAbilityLevel());
    }

    private List<EntityOutput> createUnappliedSimulations(final GetEntityInput input) {
        if (input.getSimulations() == null) {
            return Collections.emptyList();
        }
        final List<EntityOutput> simulatedEntities = new ArrayList<>();

        input.getSimulations().forEach(simulation ->
                simulation.getEntities().stream()
                        .filter(entity -> entity.getType() == PRODUCTIVITY)
                        .forEach(entity -> {
                            entity.getValues().forEach(quantityByDate ->
                                    simulatedEntities.add(EntityOutput.builder()
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
