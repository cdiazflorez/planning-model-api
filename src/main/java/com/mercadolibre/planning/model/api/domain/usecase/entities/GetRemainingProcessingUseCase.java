package com.mercadolibre.planning.model.api.domain.usecase.entities;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.output.EntityOutput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.EntitiesUtil.toMapByProcessNameAndDate;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
@AllArgsConstructor
public class GetRemainingProcessingUseCase implements UseCase<GetEntityInput, List<EntityOutput>> {

    private final ProcessingDistributionRepository processingDistRepository;
    private final GetThroughputUseCase getThroughputUseCase;

    @Override
    public List<EntityOutput> execute(final GetEntityInput input) {

        final List<Long> throughputPerHourList = new ArrayList<>();

        getThroughputUseCase.execute(GetEntityInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .entityType(THROUGHPUT)
                .processName(List.of(PICKING, PACKING))
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo().plusHours(2))
                .build())
                .stream()
                .collect(
                        groupingBy(
                            entityOutput -> entityOutput.getDate().withFixedOffsetZone(),
                            TreeMap::new,
                            toList()
                )).forEach((entityDate, throughputList) ->
                    throughputPerHourList.add(throughputList
                            .stream()
                            .map(EntityOutput::getValue)
                            .mapToLong(v -> v)
                            .min()
                            .orElse(0)));

        final double totalThroughput = throughputPerHourList
                .stream()
                .mapToDouble(Long::doubleValue)
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
