package com.mercadolibre.planning.model.api.domain.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mercadolibre.planning.model.api.util.DateUtils.fromDate;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.SimulationUtils.createSimulationMap;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static java.time.ZoneOffset.UTC;
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
        final Workflow workflow = input.getWorkflow();
        final List<ProcessingDistributionView> processingDistributions =
                findProcessingDistributionBy(input);

        final List<CurrentProcessingDistribution> currentProcessingDistributions =
                currentPDistributionRepository
                        .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                                input.getWarehouseId(),
                                input.getWorkflow(),
                                ProcessingType.ACTIVE_WORKERS,
                                input.getProcessName(),
                                input.getDateFrom(),
                                input.getDateTo());

        final Map<ProcessName, Map<ZonedDateTime, Long>> simulations = createSimulationMap(
                input.getSimulations(), HEADCOUNT);

        final List<EntityOutput> entities = new ArrayList<>();

        processingDistributions.forEach(pd ->
                entities.add(EntityOutput.builder()
                        .workflow(workflow)
                        .date(pd.getDate().toInstant().atZone(UTC))
                        .metricUnit(pd.getQuantityMetricUnit())
                        .processName(pd.getProcessName())
                        .source(FORECAST)
                        .value(simulations.containsKey(pd.getProcessName())
                                ? simulations.get(pd.getProcessName()).getOrDefault(
                                        fromDate(pd.getDate()), pd.getQuantity())
                                : pd.getQuantity()
                        )
                        .type(pd.getType())
                        .build()));

        currentProcessingDistributions.forEach(cpd ->
                entities.add(EntityOutput.builder()
                        .workflow(workflow)
                        .date(cpd.getDate())
                        .value(cpd.getQuantity())
                        .source(SIMULATION)
                        .processName(cpd.getProcessName())
                        .metricUnit(cpd.getQuantityMetricUnit())
                        .type(cpd.getType())
                        .build()));

        return entities;
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

    private Set<String> getProcessingTypeAsStringOrNull(
            final Set<ProcessingType> processingTypes) {
        return processingTypes == null
                ? null
                : processingTypes.stream().map(Enum::name).collect(toSet());
    }
}
