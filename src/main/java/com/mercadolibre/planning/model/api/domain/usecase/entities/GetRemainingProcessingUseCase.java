package com.mercadolibre.planning.model.api.domain.usecase.entities;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
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
        // We should be querying for each process name coming on input, except in the case of
        // WAVING where we need TPH from PICKING and PACKING in order to calculate the
        // remaining processing in UNITS.
        final List<EntityOutput> throughput = getThroughputUseCase.execute(GetEntityInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .entityType(THROUGHPUT)
                .processName(List.of(PICKING, PACKING))
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build());

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

        return getRemainingProcessingUnits(input, throughput, remainingProcessing);
    }

    private List<EntityOutput> getRemainingProcessingUnits(
            final GetEntityInput input,
            final List<EntityOutput> throughput,
            final List<ProcessingDistributionView> remainingProcessing) {

        final Map<ZonedDateTime, List<EntityOutput>> throughputMap = throughput.stream()
                .collect(groupingBy(
                        (e) -> e.getDate().withFixedOffsetZone(),
                        TreeMap::new,
                        toList()
                ));

        final Map<ProcessName, Map<ZonedDateTime, EntityOutput>> remainingProcessingMap =
                toMapByProcessNameAndDate(remainingProcessing.stream()
                        .map(pd -> EntityOutput.fromProcessingDistributionView(pd))
                        .collect(toList())
                );

        final List<EntityOutput> remainingProcessingInUnits = new ArrayList<>();

        remainingProcessingMap.forEach((processName, remainingProcessingByProcess) ->
                remainingProcessingByProcess.forEach((dateTime, remainingProcessingByDate) -> {

                    final long remainingProcessingInMinutes = remainingProcessingByDate.getValue();
                    final long throughputPerHour = throughputMap.get(dateTime).stream()
                            .map(EntityOutput::getValue)
                            .mapToLong(v -> v)
                            .min()
                            .orElse(0);

                    final long value =
                            Math.round((throughputPerHour / 60.0) * remainingProcessingInMinutes);

                    remainingProcessingInUnits.add(EntityOutput.builder()
                            .workflow(input.getWorkflow())
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
