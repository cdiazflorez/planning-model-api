package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationsUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;

import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.DEFERRAL;
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

    private final GetCycleTimeUseCase getCycleTimeUseCase;

    private final GetConfigurationsUseCase getConfigurationsUseCase;

    @Override
    public List<CptProjectionOutput> execute(final GetDeliveryPromiseProjectionInput input) {
        final String logisticCenterId = input.getWarehouseId();
        final CptProjectionInput projectionInput = CptProjectionInput.builder()
                .workflow(input.getWorkflow())
                .logisticCenterId(logisticCenterId)
                .capacity(getMaxCapacity(input))
                .backlog(input.getBacklog())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .planningUnits(emptyList())
                .projectionType(DEFERRAL)
                .currentDate(getCurrentUtcDate())
                .build();

        final List<CptProjectionOutput> projections = projectionUseCase.execute(projectionInput);

        return addProcessingTimes(logisticCenterId, projections);
    }

    private List<CptProjectionOutput> addProcessingTimes(
            final String warehouseId, final List<CptProjectionOutput> projections) {

        final List<Configuration> configurations = getConfigurationsUseCase.execute(warehouseId);

        projections.stream().forEach(projectionOutput -> {

            final Configuration cycleTime = getCycleTimeUseCase.execute(
                    GetCycleTimeInput.builder()
                            .cptDate(projectionOutput.getDate())
                            .configurations(configurations)
                            .build());

            projectionOutput.setProcessingTime(
                    new ProcessingTime(cycleTime.getValue(), cycleTime.getMetricUnit()));
        });

        return projections;
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

}
