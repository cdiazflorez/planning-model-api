package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.util.DateUtils.fromDate;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

@AllArgsConstructor
@Service
public class GetHeadcountEntityUseCase implements GetEntityUseCase {

    private final ProcessingDistributionRepository processingDistRepository;
    private final CurrentProcessingDistributionRepository currentPDistributionRepository;

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        if (input.getSource() == FORECAST) {
            return getForecastHeadcount(input);
        } else {
            return getSimulationHeadcount(input);
        }
    }

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == HEADCOUNT;
    }

    private List<EntityOutput> getForecastHeadcount(final GetEntityInput input) {
        final List<ProcessingDistributionView> processingDistributions =
                findProcessingDistributionBy(input);

        return processingDistributions.stream()
                .map(p -> EntityOutput.builder()
                        .workflow(input.getWorkflow())
                        .date(fromDate(p.getDate()))
                        .processName(p.getProcessName())
                        .value(p.getQuantity())
                        .metricUnit(p.getQuantityMetricUnit())
                        .type(p.getType())
                        .source(FORECAST)
                        .build())
                .collect(toList());
    }

    private List<EntityOutput> getSimulationHeadcount(final GetEntityInput input) {
        final List<CurrentProcessingDistribution> currentProcessingDistributions =
                findCurrentProcessingDistributionBy(input);

        final List<EntityOutput> entities = getForecastHeadcount(input);
        final List<EntityOutput> inputSimulatedEntities = createUnappliedSimulations(input);

        currentProcessingDistributions.forEach(cpd -> {
            if (noSimulationExistsWithSameProperties(inputSimulatedEntities, cpd)) {
                entities.add(
                        EntityOutput.builder()
                                .workflow(input.getWorkflow())
                                .date(cpd.getDate())
                                .value(cpd.getQuantity())
                                .source(SIMULATION)
                                .processName(cpd.getProcessName())
                                .metricUnit(cpd.getQuantityMetricUnit())
                                .type(cpd.getType())
                                .build());

            }
        });

        entities.addAll(inputSimulatedEntities);
        return new ArrayList<>(entities);
    }

    private boolean noSimulationExistsWithSameProperties(final List<EntityOutput> entities,
                                                         final CurrentProcessingDistribution cpd) {
        return entities.stream().noneMatch(entityOutput -> entityOutput.getSource() == SIMULATION
                && entityOutput.getProcessName() == cpd.getProcessName()
                && entityOutput.getWorkflow() == cpd.getWorkflow()
                && entityOutput.getDate().withFixedOffsetZone()
                .isEqual(cpd.getDate().withFixedOffsetZone()));
    }

    private List<ProcessingDistributionView> findProcessingDistributionBy(
            final GetEntityInput input) {

        return processingDistRepository
                .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        input.getWarehouseId(),
                        input.getWorkflow().name(),
                        getProcessingTypeAsStringOrNull(input.getProcessingType()),
                        input.getProcessName().stream().map(Enum::name).collect(toList()),
                        input.getDateFrom(),
                        input.getDateTo(),
                        getForecastWeeks(input.getDateFrom(), input.getDateTo()));
    }

    private List<CurrentProcessingDistribution> findCurrentProcessingDistributionBy(
            final GetEntityInput input) {

        return currentPDistributionRepository
                .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        ProcessingType.ACTIVE_WORKERS,
                        input.getProcessName(),
                        input.getDateFrom(),
                        input.getDateTo());
    }

    private Set<String> getProcessingTypeAsStringOrNull(
            final Set<ProcessingType> processingTypes) {
        return processingTypes == null
                ? null
                : processingTypes.stream().map(Enum::name).collect(toSet());
    }

    private List<EntityOutput> createUnappliedSimulations(final GetEntityInput input) {
        if (input.getSimulations() == null) {
            return Collections.emptyList();
        }

        final List<EntityOutput> simulatedEntities = new ArrayList<>();

        input.getSimulations().forEach(simulation ->
                simulation.getEntities().stream()
                        .filter(entity -> entity.getType() == HEADCOUNT)
                        .forEach(entity -> {
                            entity.getValues().forEach(quantityByDate ->
                                    simulatedEntities.add(EntityOutput.builder()
                                            .workflow(input.getWorkflow())
                                            .date(quantityByDate.getDate().withFixedOffsetZone())
                                            .metricUnit(MetricUnit.WORKERS)
                                            .processName(simulation.getProcessName())
                                            .source(SIMULATION)
                                            .value(quantityByDate.getQuantity())
                                            .type(ProcessingType.ACTIVE_WORKERS)
                                            .build()));
                        }));

        return simulatedEntities;
    }
}
