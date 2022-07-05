package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.MetricUnit;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.SlaProjectionInput;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class DeliveryPromiseCalculator {

  private DeliveryPromiseCalculator() {
  }

  public static List<DeliveryPromiseProjectionOutput> calculate(final ZonedDateTime dateFrom,
                                                                final ZonedDateTime dateTo,
                                                                final ZonedDateTime currentDate,
                                                                final List<Backlog> currentBacklog,
                                                                final Map<ZonedDateTime, Integer> maxCapacity,
                                                                final List<GetSlaByWarehouseOutput> allCptByWarehouse,
                                                                final Map<ZonedDateTime, Long> cycleTimeByCpt) {

    final List<CptCalculationOutput> allCptProjectionCalculated = CalculateCptProjectionUseCase.execute(SlaProjectionInput.builder()
        .capacity(maxCapacity)
        .backlog(currentBacklog)
        .dateFrom(dateFrom)
        .dateTo(dateTo)
        .plannedUnits(emptyList())
        .slaByWarehouse(allCptByWarehouse)
        .currentDate(currentDate)
        .build());

    return calculatedDeferralCpt(allCptProjectionCalculated, allCptByWarehouse, cycleTimeByCpt, currentDate);
  }

  private static List<DeliveryPromiseProjectionOutput> calculatedDeferralCpt(final List<CptCalculationOutput> allCptProjectionCalculated,
                                                                             final List<GetSlaByWarehouseOutput> allCptByWarehouse,
                                                                             final Map<ZonedDateTime, Long> cycleTimeByCpt,
                                                                             final ZonedDateTime currentDate) {

    final Map<ZonedDateTime, ProcessingTime> processingTimeByCpt = allCptByWarehouse.stream()
        .collect(
            toMap(
                item -> item.getDate().withFixedOffsetZone(),
                GetSlaByWarehouseOutput::getProcessingTime));

    boolean isDeferredByCascade = false;

    final List<DeliveryPromiseProjectionOutput> allCptDeferralCalculated = new ArrayList<>();

    allCptProjectionCalculated.sort(comparing(CptCalculationOutput::getDate, reverseOrder()));

    for (final CptCalculationOutput cptCalculated : allCptProjectionCalculated) {
      final long cycleTime = cycleTimeByCpt.get(cptCalculated.getDate());
      final long processingTime = processingTimeByCpt.get(cptCalculated.getDate()).getValue();

      final ZonedDateTime cutOff = cptCalculated.getDate().minusMinutes(cycleTime);
      final ZonedDateTime payBefore = cptCalculated.getDate().minusMinutes(processingTime);

      final boolean isProjectionOver24h = cptCalculated.getProjectedEndDate() == null;
      final boolean isDeferralByCutOff = !isProjectionOver24h && cptCalculated.getProjectedEndDate().isAfter(cutOff);
      final boolean isTimeForDeferral = currentDate.isBefore(payBefore);

      final boolean isDeferredByCap5 = isTimeForDeferral && (isProjectionOver24h || isDeferralByCutOff);
      isDeferredByCascade = isDeferredByCascade || isDeferredByCap5;

      final boolean isDeferred = isTimeForDeferral && (isDeferredByCap5 || isDeferredByCascade);

      allCptDeferralCalculated.add(
          createCpt(
              cptCalculated,
              cutOff,
              cycleTime,
              payBefore,
              isDeferred,
              isDeferredByCap5
          )
      );
    }

    return allCptDeferralCalculated.stream()
        .sorted(comparing(DeliveryPromiseProjectionOutput::getDate))
        .collect(toList());
  }

  private static DeliveryPromiseProjectionOutput createCpt(final CptCalculationOutput calculation,
                                                           final ZonedDateTime etdCutoff,
                                                           final long cycleTime,
                                                           final ZonedDateTime payBefore,
                                                           final boolean isDeferred,
                                                           final boolean isDeferredByCap5) {

    return new DeliveryPromiseProjectionOutput(
        calculation.getDate(),
        calculation.getProjectedEndDate(),
        calculation.getRemainingQuantity(),
        etdCutoff,
        new ProcessingTime(cycleTime, MetricUnit.MINUTES),
        payBefore,
        isDeferred,
        getDeferralReason(isDeferred, isDeferredByCap5));
  }

  private static DeferralStatus getDeferralReason(final boolean isDeferred,
                                                  final boolean isDeferredByCap5) {

    if (!isDeferred) {
      return DeferralStatus.NOT_DEFERRED;
    } else if (isDeferredByCap5) {
      return DeferralStatus.DEFERRED_CAP_MAX;
    } else {
      return DeferralStatus.DEFERRED_CASCADE;
    }
  }
}
