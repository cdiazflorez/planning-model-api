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
import com.mercadolibre.planning.model.api.web.controller.request.Source;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
public class GetHeadcountEntityUseCase implements GetEntityUseCase {

    private final ProcessingDistributionRepository processingDistRepository;
    private final CurrentProcessingDistributionRepository currentPDistributionRepository;

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {
        if (input.getSource() == null || input.getSource() == FORECAST) {
            return getForecastHeadcount(input);
        } else {
            return getSimulationHeadcount(input);
        }
    }

    private List<EntityOutput> getForecastHeadcount(final GetEntityInput input) {
        final List<ProcessingDistributionView> processingDistributions = processingDistRepository
                .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        input.getWarehouseId(),
                        input.getWorkflow().name(),
                        ProcessingType.ACTIVE_WORKERS.name(),
                        input.getProcessName().stream().map(Enum::name).collect(toList()),
                        input.getDateFrom(),
                        input.getDateTo(),
                        getForecastWeeks(input.getDateFrom(), input.getDateTo()));

        return processingDistributions.stream()
                .map(p -> EntityOutput.builder()
                        .workflow(input.getWorkflow())
                        .date(ofInstant(p.getDate().toInstant(), UTC))
                        .processName(p.getProcessName())
                        .value(p.getQuantity())
                        .metricUnit(p.getQuantityMetricUnit())
                        .source(FORECAST)
                        .build())
                .collect(toList());
    }

    private List<EntityOutput> getSimulationHeadcount(final GetEntityInput input) {
        final List<ProcessingDistributionView> processingDistributions = processingDistRepository
                .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        input.getWarehouseId(),
                        input.getWorkflow().name(),
                        ProcessingType.ACTIVE_WORKERS.name(),
                        input.getProcessName().stream().map(Enum::name).collect(toList()),
                        input.getDateFrom(),
                        input.getDateTo(),
                        getForecastWeeks(input.getDateFrom(), input.getDateTo()));

        final List<CurrentProcessingDistribution> currentProcessingDistributions =
                currentPDistributionRepository
                        .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                                input.getWarehouseId(),
                                input.getWorkflow(),
                                ProcessingType.ACTIVE_WORKERS,
                                input.getProcessName(),
                                input.getDateFrom(),
                                input.getDateTo());

        return processingDistributions.stream()
                .map(p -> getEntityOutput(input, currentProcessingDistributions, p))
                .collect(toList());
    }

    private EntityOutput getEntityOutput(final GetEntityInput input,
                                         final List<CurrentProcessingDistribution> currentDistList,
                                         final ProcessingDistributionView processingDistribution) {
        final ZonedDateTime date = processingDistribution.getDate().toInstant().atZone(UTC);
        final Optional<CurrentProcessingDistribution> currentDistributionOptional =
                currentDistList.stream()
                        .filter(t -> t.getDate()
                                .isEqual(date)
                                && t.getProcessName() == processingDistribution.getProcessName())
                        .findFirst();
        final long quantity = currentDistributionOptional
                .map(CurrentProcessingDistribution::getQuantity)
                .orElseGet(processingDistribution::getQuantity);
        final MetricUnit quantityMetricUnit = currentDistributionOptional
                .map(CurrentProcessingDistribution::getQuantityMetricUnit)
                .orElseGet(processingDistribution::getQuantityMetricUnit);
        final Source source = currentDistributionOptional
                .map(t -> SIMULATION)
                .orElse(FORECAST);
        return EntityOutput.builder()
                .workflow(input.getWorkflow())
                .date(date)
                .processName(processingDistribution.getProcessName())
                .value(quantity)
                .metricUnit(quantityMetricUnit)
                .source(source)
                .build();
    }

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == HEADCOUNT;
    }
}
