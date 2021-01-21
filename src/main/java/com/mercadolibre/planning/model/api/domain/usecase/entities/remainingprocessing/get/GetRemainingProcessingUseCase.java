package com.mercadolibre.planning.model.api.domain.usecase.entities.remainingprocessing.get;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityInput.fromEntityOutputs;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.EntitiesUtil.toMapByProcessNameAndDate;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class GetRemainingProcessingUseCase
        implements EntityUseCase<GetEntityInput, List<EntityOutput>> {

    private final ProcessingDistributionRepository processingDistRepository;
    private final GetThroughputUseCase getThroughputUseCase;
    private final GetCapacityPerHourUseCase getCapacityPerHourUseCase;

    @Override
    public boolean supportsEntityType(final EntityType entityType) {
        return entityType == EntityType.REMAINING_PROCESSING;
    }

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {

        final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .entityType(THROUGHPUT)
                .processName(List.of(PICKING, PACKING, PACKING_WALL))
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo().plusHours(2))
                .build());

        final List<CapacityOutput> capacityOutputs = getCapacityPerHourUseCase.execute(
                fromEntityOutputs(throughput)
        );

        final double totalThroughput = capacityOutputs
                .stream()
                .mapToDouble(CapacityOutput::getValue)
                .summaryStatistics()
                .getAverage();

        final List<ProcessingDistributionView> remainingProcessing = processingDistRepository
                .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        input.getWarehouseId(),
                        input.getWorkflow().name(),
                        Set.of(REMAINING_PROCESSING.name()),
                        input.getProcessNamesAsString(),
                        input.getDateFrom(),
                        input.getDateTo(),
                        getForecastWeeks(input.getDateFrom(), input.getDateTo())
                );

        return getRemainingProcessingUnits(
                input.getWorkflow(),
                totalThroughput,
                remainingProcessing);
    }

    private List<EntityOutput> getRemainingProcessingUnits(
            final Workflow workflow,
            final double totalThroughput,
            final List<ProcessingDistributionView> remainingProcessing) {

        final Map<ProcessName, Map<ZonedDateTime, EntityOutput>> remainingProcessingMap =
                toMapByProcessNameAndDate(remainingProcessing.stream()
                        .map(EntityOutput::fromProcessingDistributionView)
                        .collect(toList())
                );

        final List<EntityOutput> remainingProcessingInUnits = new ArrayList<>();

        remainingProcessingMap.forEach((processName, remainingProcessingByProcess) ->
                remainingProcessingByProcess.forEach((dateTime, remainingProcessingByDate) -> {

                    final long remainingProcessingInMinutes = remainingProcessingByDate.getValue();

                    final long value =
                            Math.round((totalThroughput / 60.0) * remainingProcessingInMinutes);

                    remainingProcessingInUnits.add(EntityOutput.builder()
                            .workflow(workflow)
                            .date(remainingProcessingByDate.getDate().withFixedOffsetZone())
                            .processName(processName)
                            .metricUnit(UNITS)
                            .value(value)
                            .source(FORECAST)
                            .type(REMAINING_PROCESSING)
                            .build());
                })
        );

        return remainingProcessingInUnits;
    }

}
