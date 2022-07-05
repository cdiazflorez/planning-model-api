package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input;

import static com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla.emptyBacklog;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.IncomingBacklog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.QuantityAtDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * Forecasted backlog by hour and sla representation.
 */
@Value
public class PlannedBacklogBySla implements IncomingBacklog<BacklogBySla> {
  private static final double MINUTES_IN_HOUR = 60.0;

  Map<Instant, BacklogBySla> plannedUnits;

  public static PlannedBacklogBySla fromPlannedUnits(final List<PlannedUnits> plannedBacklog) {
    final var distributionsByDate = plannedBacklog.stream()
        .collect(groupingBy(
                distribution -> distribution.getDateIn().toInstant(),
                toList()
            )
        )
        .entrySet()
        .stream()
        .collect(toMap(
            Map.Entry::getKey,
            entry -> {
              final var quantities = entry.getValue()
                  .stream()
                  .map(distribution -> new QuantityAtDate(distribution.getDateOut().toInstant(), (int) distribution.getTotal()))
                  .collect(toList());

              return new BacklogBySla(quantities);
            }
        ));

    return new PlannedBacklogBySla(distributionsByDate);
  }

  @Override
  public BacklogBySla get(final Instant operatingHour) {
    final var minutes = operatingHour.atZone(ZoneOffset.UTC).getMinute();
    return get(operatingHour, (int) MINUTES_IN_HOUR - minutes);
  }

  @Override
  public BacklogBySla get(final Instant dateFrom, final Instant dateTo) {
    final var minutes = ChronoUnit.MINUTES.between(dateFrom, dateTo);
    return get(dateFrom, (int) minutes);
  }

  private BacklogBySla get(final Instant dateFrom, final int minutes) {
    final var truncated = dateFrom.truncatedTo(ChronoUnit.HOURS);

    final var backlog = plannedUnits.get(truncated);
    if (backlog == null) {
      return emptyBacklog();
    }

    final var remainingHourFraction = minutes / MINUTES_IN_HOUR;
    if (remainingHourFraction == MINUTES_IN_HOUR) {
      return backlog;
    } else {
      final var distributions = backlog.getDistributions();
      return new BacklogBySla(
          distributions.stream()
              .map(quantity -> new QuantityAtDate(quantity.getDate(), (int) (quantity.getQuantity() * remainingHourFraction)))
              .collect(Collectors.toList())
      );
    }
  }
}
