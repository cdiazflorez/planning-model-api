package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.COMMAND_CENTER_DEFERRAL;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationDetailOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.SlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.exception.InvalidForecastException;
import com.mercadolibre.planning.model.api.util.TestLogisticCenterMapper;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.LongStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LongVariable"})
@Slf4j
@Component
@AllArgsConstructor
public class GetDeliveryPromiseProjectionUseCase {

    private static final int HOUR_IN_MINUTES = 60;

    private final CalculateCptProjectionUseCase calculatedProjectionUseCase;

    private final ProcessingDistributionRepository processingDistRepository;

    private final GetForecastUseCase getForecastUseCase;

    private final GetCycleTimeService getCycleTimeService;

    private final GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

    private final PlannedBacklogService plannedBacklogService;

    public List<DeliveryPromiseProjectionOutput> execute(final GetDeliveryPromiseProjectionInput input) {

        final List<GetSlaByWarehouseOutput> allCptByWarehouse =
                getSlaByWarehouseOutboundService.execute(new GetSlaByWarehouseInput(TestLogisticCenterMapper
                        .toRealLogisticCenter(input.getWarehouseId()),
                        input.getDateFrom(),
                        input.getDateTo(),
                        getCptDefaultFromBacklog(input.getBacklog()),
                        input.getTimeZone()));

        final List<PlannedUnits> backlogPlanned =
                input.getProjectionType() == COMMAND_CENTER_DEFERRAL ? plannedBacklogService.getExpectedBacklog(
                        input.getWarehouseId(),
                        input.getWorkflow(),
                        input.getDateFrom(),
                        input.getDateTo(),
                        input.isApplyDeviation())
                        : emptyList();

        final List<CptCalculationOutput> allCptProjectionCalculated = calculatedProjectionUseCase.execute(SlaProjectionInput.builder()
                .workflow(input.getWorkflow())
                .logisticCenterId(input.getWarehouseId())
                .capacity(getMaxCapacity(input))
                .backlog(input.getBacklog())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .plannedUnits(backlogPlanned)
                .slaByWarehouse(allCptByWarehouse)
                .currentDate(getCurrentUtcDate())
                .build());

        final Map<ZonedDateTime, Long> cycleTimeByCpt = getCycleTimeService.execute(
                new GetCycleTimeInput(input.getWarehouseId(),
                        allCptProjectionCalculated.stream().map(CptCalculationOutput::getDate).collect(toList())));

        return calculatedDeferralCpt(allCptProjectionCalculated, allCptByWarehouse, cycleTimeByCpt, input.getProjectionType());
    }

    private List<Long> getForecastIds(final GetDeliveryPromiseProjectionInput input) {
        return getForecastUseCase.execute(new GetForecastInput(
                input.getWarehouseId(),
                input.getWorkflow(),
                input.getDateFrom(),
                input.getDateTo()
        ));
    }

    private Map<ZonedDateTime, Integer> getMaxCapacity(final GetDeliveryPromiseProjectionInput input) {

        final List<ProcessingDistributionView> processingDistributionView = processingDistRepository
                .findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        Set.of(MAX_CAPACITY.name()),
                        List.of(GLOBAL.toJson()),
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

        final int defaultCapacity = capacityByDate.values().stream()
                .max(Integer::compareTo)
                .orElseThrow(() ->
                        new InvalidForecastException(
                                input.getWarehouseId(),
                                input.getWorkflow().name())
                );

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

    private List<DeliveryPromiseProjectionOutput> calculatedDeferralCpt(final List<CptCalculationOutput> allCptProjectionCalculated,
                                                                        final List<GetSlaByWarehouseOutput> allCptByWarehouse,
                                                                        final Map<ZonedDateTime, Long> cycleTimeByCpt,
                                                                        final ProjectionType projectionType) {

        final ZonedDateTime currentDate = getCurrentUtcDate();

        final Map<ZonedDateTime, ProcessingTime> processingTimeByCpt = allCptByWarehouse.stream()
                .collect(
                        toMap(
                                item -> item.getDate().withFixedOffsetZone(),
                                GetSlaByWarehouseOutput::getProcessingTime));

        boolean isDeferredByCap5 = false;

        final List<DeliveryPromiseProjectionOutput> allCptDeferralCalculated = new ArrayList<>();

        allCptProjectionCalculated.sort(comparing(CptCalculationOutput::getDate, reverseOrder()));

        for (final CptCalculationOutput cptCalculated : allCptProjectionCalculated) {
            int unitsToDefer = 0;
            final long cycleTime = cycleTimeByCpt.get(cptCalculated.getDate());
            final long processingTime = processingTimeByCpt.get(cptCalculated.getDate()).getValue();

            final ZonedDateTime cutOff = cptCalculated.getDate().minusMinutes(cycleTime);
            final ZonedDateTime payBefore = cptCalculated.getDate().minusMinutes(processingTime);

            final boolean isProjectionOver24h = cptCalculated.getProjectedEndDate() == null;
            final boolean isDeferralByCutOff = !isProjectionOver24h && cptCalculated.getProjectedEndDate().isAfter(cutOff);
            final boolean isTimeForDeferral = currentDate.isBefore(payBefore);

            if (isTimeForDeferral && (isProjectionOver24h || isDeferralByCutOff)) {
                isDeferredByCap5 = true;
            }

            if (projectionType == COMMAND_CENTER_DEFERRAL && isDeferredByCap5 && isTimeForDeferral) {
                unitsToDefer = getUnitsToDefer(cptCalculated, cutOff);
            }

            allCptDeferralCalculated.add(createCpt(
                    cptCalculated,
                    cutOff,
                    cycleTime,
                    payBefore,
                    isDeferredByCap5 && isTimeForDeferral,
                    unitsToDefer));
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

    private int getUnitsToDefer(final CptCalculationOutput calculation,
                                final ZonedDateTime cutOff) {

        int remainingQuantity = 0;
        int unitsToDefer = 0;

        for (final CptCalculationDetailOutput projectionRecord : calculation.getCalculationDetails()) {
            if (cutOff.truncatedTo(HOURS).isEqual(projectionRecord.getOperationHour())) {
                final int minutes = (int) MINUTES.between(projectionRecord.getOperationHour(), cutOff);
                final int unitsBeingProcessed = minutes * projectionRecord.getUnitsBeingProcessed() / HOUR_IN_MINUTES;
                remainingQuantity = projectionRecord.getCurrentBacklog() - unitsBeingProcessed;
                break;
            }
        }

        if (remainingQuantity > 0) {
            final int totalPlannedBacklog = calculation.getTotalPlannedBacklog();
            final boolean isRemainingMin = remainingQuantity < totalPlannedBacklog;
            unitsToDefer = isRemainingMin ? remainingQuantity : totalPlannedBacklog;
        }

        return unitsToDefer;
    }

    private DeliveryPromiseProjectionOutput createCpt(final CptCalculationOutput calculation,
                                                      final ZonedDateTime etdCutoff,
                                                      final long cycleTime,
                                                      final ZonedDateTime payBefore,
                                                      final boolean isDeferred,
                                                      final int unitsToDefer) {
        return new DeliveryPromiseProjectionOutput(
                calculation.getDate(),
                calculation.getProjectedEndDate(),
                calculation.getRemainingQuantity(),
                etdCutoff,
                new ProcessingTime(cycleTime, MetricUnit.MINUTES),
                payBefore,
                isDeferred,
                unitsToDefer);
    }
}
