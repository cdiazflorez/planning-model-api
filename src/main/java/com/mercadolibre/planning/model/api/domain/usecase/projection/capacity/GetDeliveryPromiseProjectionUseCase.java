package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationCycleTimeUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

@SuppressWarnings("PMD.ExcessiveImports")
@Component
@AllArgsConstructor
public class GetDeliveryPromiseProjectionUseCase implements
        UseCase<GetDeliveryPromiseProjectionInput, List<CptProjectionOutput>> {

    private final CalculateCptProjectionUseCase projectionUseCase;

    private final ProcessingDistributionRepository processingDistRepository;

    private final GetForecastUseCase getForecastUseCase;

    private final GetConfigurationCycleTimeUseCase getConfigurationUseCase;

    @Override
    public List<CptProjectionOutput> execute(final GetDeliveryPromiseProjectionInput input) {
        final CptProjectionInput projectionInput = CptProjectionInput.builder()
                .workflow(input.getWorkflow())
                .logisticCenterId(input.getWarehouseId())
                .capacity(getMaxCapacity(input))
                .backlog(input.getBacklog())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .planningUnits(emptyList())
                .build();

        final List<Configuration> configurations = getConfigurationUseCase
                .execute(input.getWarehouseId());

        return getCycleTime(
                projectionUseCase.execute(projectionInput),
                configurations, input);
    }

    private List<Long> getForecastIds(final GetDeliveryPromiseProjectionInput input) {
        return getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build()
        );
    }

    private Map<ZonedDateTime, Integer> getMaxCapacity(
            final GetDeliveryPromiseProjectionInput input) {

        final List<ProcessingDistributionView> processingDistributionView =
                processingDistRepository
                        .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                                Set.of(ProcessingType.MAX_CAPACITY.name()),
                                List.of(ProcessName.GLOBAL.toJson()),
                                input.getDateFrom(),
                                input.getDateTo(),
                                getForecastIds(input));

        final Map<Instant, Integer> capacityByDate = processingDistributionView.stream()
                .collect(Collectors.toMap(
                        o -> o.getDate().toInstant().truncatedTo(SECONDS),
                        o -> (int) o.getQuantity(),
                        (intA, intB) -> intB));

        final int defaultCapacity = capacityByDate.values()
                .stream().max(Integer::compareTo)
                .orElseThrow(NoSuchElementException::new);

        final Set<Instant> capacityHours = getCapacityHours(
                input.getDateFrom(), input.getDateTo());

        return capacityHours.stream().collect(Collectors.toMap(
                o -> ZonedDateTime.from(o.atZone(ZoneOffset.UTC)),
                o -> capacityByDate.getOrDefault(o, defaultCapacity),
                (intA, intB) -> intB,
                TreeMap::new
        ));
    }

    private Set<Instant> getCapacityHours(final ZonedDateTime dateFrom,
                                          final Temporal dateTo) {

        final Duration dur = Duration.between(dateFrom, dateTo);
        return LongStream.range(0, dur.toHours())
                .mapToObj(i -> dateFrom.plusHours(i).truncatedTo(SECONDS).toInstant())
                .collect(toSet());
    }

    private List<CptProjectionOutput> getCycleTime(final List<CptProjectionOutput> projections,
                                                   final List<Configuration> configurations,
                                                   final GetDeliveryPromiseProjectionInput input) {

        projections.stream().forEach(projectionOutput -> {

            if (isSpecialCase(input.getWarehouseId(), projectionOutput)) {
                projectionOutput.setProcessingTime(new ProcessingTime(120, MetricUnit.MINUTES));
            } else {

                final String cycleTimeKey = getCycleTimeKey(projectionOutput);

                final Optional<Configuration> cycleTimeConfig = configurations.stream()
                        .filter(configuration -> cycleTimeKey.equals(configuration.getKey()))
                        .findFirst();

                final long cycleTimeValue = cycleTimeConfig
                        .map(Configuration::getValue)
                        .orElseGet(() -> getProcessingTimeConfig(configurations).getValue());

                final MetricUnit cycleTimeMetric = cycleTimeConfig
                        .map(Configuration::getMetricUnit)
                        .orElseGet(() -> getProcessingTimeConfig(configurations).getMetricUnit());

                projectionOutput.setProcessingTime(
                        new ProcessingTime(cycleTimeValue, cycleTimeMetric));

            }
        });

        return projections;
    }

    private String getCycleTimeKey(final CptProjectionOutput cptProjectionOutput) {
        final ZonedDateTime cptDate = cptProjectionOutput.getDate();
        return String.format("cycle_time_%02d_%02d", cptDate.getHour(), cptDate.getMinute());
    }

    private boolean isSpecialCase(final String warehouseId,
                                  final CptProjectionOutput cptProjectionOutput) {

        final ZonedDateTime cptDate = cptProjectionOutput.getDate();
        final boolean isWeekend = Set.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                .contains(cptDate.getDayOfWeek());

        return isWeekend
                && "MXJC01".equals(warehouseId)
                && cptDate.getHour() == 18
                && cptDate.getMinute() == 0;
    }

    private Configuration getProcessingTimeConfig(final List<Configuration> configurations) {
        return configurations.stream()
                .filter(configuration -> "processing_time".equals(configuration.getKey()))
                .findFirst()
                .get();
    }
}
