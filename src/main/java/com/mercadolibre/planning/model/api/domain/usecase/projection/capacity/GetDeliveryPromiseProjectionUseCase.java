package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.LongStream;

import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.DEFERRAL;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@SuppressWarnings("PMD.ExcessiveImports")
@Component
@AllArgsConstructor
public class GetDeliveryPromiseProjectionUseCase {

    private final CalculateCptProjectionUseCase projectionUseCase;

    private final ProcessingDistributionRepository processingDistRepository;

    private final GetForecastUseCase getForecastUseCase;

    private final GetCycleTimeUseCase getCycleTimeUseCase;

    private final GetCptByWarehouseUseCase getCptByWarehouseUseCase;

    public List<DeliveryPromiseProjectionOutput> execute(
            final GetDeliveryPromiseProjectionInput input) {

        final List<GetCptByWarehouseOutput> allCptByWarehouse =
                getCptByWarehouseUseCase.execute(
                        new GetCptByWarehouseInput(
                                input.getWarehouseId(),
                                input.getDateFrom(),
                                input.getDateTo(),
                                getCptDefaultFromBacklog(input.getBacklog()),
                                input.getTimeZone()));

        final CptProjectionInput projectionInput =
                CptProjectionInput.builder()
                        .workflow(input.getWorkflow())
                        .logisticCenterId(input.getWarehouseId())
                        .capacity(getMaxCapacity(input))
                        .backlog(input.getBacklog())
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .planningUnits(emptyList())
                        .projectionType(DEFERRAL)
                        .cptByWarehouse(allCptByWarehouse)
                        .currentDate(getCurrentUtcDate())
                        .build();

        final List<CptCalculationOutput> allCptProjectionCalculated =
                projectionUseCase.execute(projectionInput);

        final Map<ZonedDateTime, Configuration> cycleTimeByCpt =
                getCycleTimeUseCase.execute(
                        new GetCycleTimeInput(
                                input.getWarehouseId(),
                                allCptProjectionCalculated.stream()
                                        .map(CptCalculationOutput::getDate)
                                        .collect(toList())));

        return calculatedDeferralCpt(allCptProjectionCalculated, allCptByWarehouse, cycleTimeByCpt);
    }

    private List<Long> getForecastIds(final GetDeliveryPromiseProjectionInput input) {
        return getForecastUseCase.execute(
                GetForecastInput.builder()
                        .workflow(input.getWorkflow())
                        .warehouseId(input.getWarehouseId())
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .build());
    }

    private Map<ZonedDateTime, Integer> getMaxCapacity(
            final GetDeliveryPromiseProjectionInput input) {

        final List<ProcessingDistributionView> processingDistributionView =
                processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        Set.of(ProcessingType.MAX_CAPACITY.name()),
                        List.of(ProcessName.GLOBAL.toJson()),
                        input.getDateFrom(),
                        input.getDateTo(),
                        getForecastIds(input));

        final Map<Instant, Integer> capacityByDate =
                processingDistributionView.stream()
                        .collect(
                                toMap(
                                        o -> o.getDate().toInstant().truncatedTo(SECONDS),
                                        o -> (int) o.getQuantity(),
                                        (intA, intB) -> intB));

        final int defaultCapacity =
                capacityByDate.values().stream()
                        .max(Integer::compareTo)
                        .orElseThrow(NoSuchElementException::new);

        final Set<Instant> capacityHours = getCapacityHours(input.getDateFrom(), input.getDateTo());

        return capacityHours.stream()
                .collect(
                        toMap(
                                o -> ZonedDateTime.from(o.atZone(ZoneOffset.UTC)),
                                o -> capacityByDate.getOrDefault(o, defaultCapacity),
                                (intA, intB) -> intB,
                                TreeMap::new));
    }

    private Set<Instant> getCapacityHours(final ZonedDateTime dateFrom, final Temporal dateTo) {

        final Duration dur = Duration.between(dateFrom, dateTo);
        return LongStream.range(0, dur.toHours())
                .mapToObj(i -> dateFrom.plusHours(i).truncatedTo(SECONDS).toInstant())
                .collect(toSet());
    }

    private List<DeliveryPromiseProjectionOutput> calculatedDeferralCpt(
            final List<CptCalculationOutput> allCptProjectionCalculated,
            final List<GetCptByWarehouseOutput> allCptByWarehouse,
            final Map<ZonedDateTime, Configuration> cycleTimeByCpt) {

        final ZonedDateTime currentDate = getCurrentUtcDate();

        final Map<ZonedDateTime, ProcessingTime> processingTimeByCpt =
                allCptByWarehouse.stream()
                        .collect(
                                toMap(
                                        item -> item.getDate().withFixedOffsetZone(),
                                        GetCptByWarehouseOutput::getProcessingTime));

        boolean isDeferredByCap5 = false;

        final List<DeliveryPromiseProjectionOutput> allCptDeferralCalculated = new ArrayList<>();

        allCptProjectionCalculated.sort(comparing(CptCalculationOutput::getDate, reverseOrder()));

        for (final CptCalculationOutput cptCalculated : allCptProjectionCalculated) {
            final long cycleTime = cycleTimeByCpt.get(cptCalculated.getDate()).getValue();
            final long processingTime = processingTimeByCpt.get(cptCalculated.getDate()).getValue();

            final ZonedDateTime cutOff = cptCalculated.getDate().minusMinutes(cycleTime);
            final ZonedDateTime payBefore = cptCalculated.getDate().minusMinutes(processingTime);

            final boolean isProjectionOver24h = cptCalculated.getProjectedEndDate() == null;
            final boolean isDeferralByCutOff =
                    !isProjectionOver24h && cptCalculated.getProjectedEndDate().isAfter(cutOff);
            final boolean isTimeForDeferral = currentDate.isBefore(payBefore);

            if (isTimeForDeferral && (isProjectionOver24h || isDeferralByCutOff)) {
                isDeferredByCap5 = true;
            }

            allCptDeferralCalculated.add(
                    createCpt(
                            cptCalculated,
                            cutOff,
                            cycleTime,
                            payBefore,
                            isDeferredByCap5 && isTimeForDeferral));
        }

        return allCptDeferralCalculated.stream()
                .sorted(comparing(DeliveryPromiseProjectionOutput::getDate))
                .collect(toList());
    }

    private List<ZonedDateTime> getCptDefaultFromBacklog(final List<Backlog> backlogs) {

        return backlogs == null
                ? emptyList()
                : backlogs.stream().map(Backlog::getDate).distinct().collect(toList());
    }

    private DeliveryPromiseProjectionOutput createCpt(
            final CptCalculationOutput calculation,
            final ZonedDateTime etdCutoff,
            final long cycleTime,
            final ZonedDateTime payBefore,
            final boolean isDeferred) {

        return new DeliveryPromiseProjectionOutput(
                calculation.getDate(),
                calculation.getProjectedEndDate(),
                calculation.getRemainingQuantity(),
                etdCutoff,
                new ProcessingTime(cycleTime, MetricUnit.MINUTES),
                payBefore,
                isDeferred);
    }
}
