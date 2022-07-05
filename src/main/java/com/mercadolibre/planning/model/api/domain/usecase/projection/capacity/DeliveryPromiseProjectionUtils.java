package com.mercadolibre.planning.model.api.domain.usecase.projection.capacity;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.exception.InvalidForecastException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public final class DeliveryPromiseProjectionUtils {
  private DeliveryPromiseProjectionUtils() {

  }

  static List<ZonedDateTime> getCptDefaultFromBacklog(final List<Backlog> backlogs) {
    return backlogs == null
        ? emptyList()
        : backlogs.stream().map(Backlog::getDate).distinct().collect(toList());
  }

  static List<ZonedDateTime> getSlasToBeProjectedFromBacklogAndKnowSlas(final List<Backlog> backlogs,
                                                                        final List<GetSlaByWarehouseOutput> allCptByWarehouse) {
    return Stream.of(
            backlogs.stream()
                .map(Backlog::getDate),
            allCptByWarehouse.stream()
                .map(GetSlaByWarehouseOutput::getDate)
        )
        .flatMap(Function.identity())
        .map(date -> date.withZoneSameInstant(ZoneOffset.UTC))
        .distinct()
        .collect(toList());
  }

  static Map<ZonedDateTime, Integer> toMaxCapacityByDate(final String logisticCenterId,
                                                         final Workflow workflow,
                                                         final ZonedDateTime dateFrom,
                                                         final ZonedDateTime dateTo,
                                                         final List<ProcessingDistributionView> capacities) {
    final Map<Instant, Integer> capacityByDate =
        capacities.stream()
            .collect(
                toMap(
                    o -> o.getDate().toInstant().truncatedTo(SECONDS),
                    o -> (int) o.getQuantity(),
                    (intA, intB) -> intB)
            );

    final int defaultCapacity = capacityByDate.values()
        .stream()
        .max(Integer::compareTo)
        .orElseThrow(() -> new InvalidForecastException(logisticCenterId, workflow.name()));

    final Set<Instant> capacityHours = getProcessingCapacityInflectionPointsBetween(dateFrom, dateTo);

    return capacityHours.stream()
        .collect(
            toMap(
                o -> ZonedDateTime.from(o.atZone(ZoneOffset.UTC)),
                o -> capacityByDate.getOrDefault(o, defaultCapacity),
                (intA, intB) -> intB,
                TreeMap::new)
        );
  }

  private static Set<Instant> getProcessingCapacityInflectionPointsBetween(final ZonedDateTime dateFrom, final Temporal dateTo) {
    final Duration intervalBetweenDates = Duration.between(dateFrom, dateTo);
    final ZonedDateTime baseDate = dateFrom.truncatedTo(SECONDS);
    return LongStream.range(0, intervalBetweenDates.toHours())
        .mapToObj(i -> baseDate.plusHours(i).toInstant())
        .collect(toSet());
  }
}
