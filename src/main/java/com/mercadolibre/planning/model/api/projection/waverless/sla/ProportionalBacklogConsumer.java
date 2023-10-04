package com.mercadolibre.planning.model.api.projection.waverless.sla;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.BacklogConsume;
import com.mercadolibre.flow.projection.tools.services.entities.context.Consumer;
import com.mercadolibre.planning.model.api.util.MathUtil;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ProportionalBacklogConsumer implements Consumer {

  @Override
  public BacklogConsume consume(
      final Instant startingDate,
      final Instant endingDate,
      final Backlog backlogToConsume,
      final int tph
  ) {

    final Map<Instant, BacklogAndForecastByDate.QuantityWithEndDate> consumed = new HashMap<>();
    final Map<Instant, BacklogAndForecastByDate.Quantity> leftOver = new HashMap<>();

    final long inflectionPointSize = ChronoUnit.SECONDS.between(startingDate, endingDate);

    final Map<Instant, Backlog> backlogsMap = new TreeMap<>(((BacklogAndForecastByDate) backlogToConsume).getBacklogs());

    final Map<Instant, Long> tphByDate = getTphByDate(tph, backlogsMap);

    int remainingTph = tph;
    long tphForecast = tph - tphByDate.values().stream().mapToLong(Long::longValue).sum();
    for (Map.Entry<Instant, ? extends Backlog> entry : backlogsMap.entrySet()) {
      BacklogAndForecastByDate.BacklogAndForecast quantities = (BacklogAndForecastByDate.BacklogAndForecast) entry.getValue();

      if (quantities.total() != 0) {
        final Instant processingEndDate = getProjectedEndDate(
            startingDate,
            inflectionPointSize,
            quantities.total(),
            remainingTph,
            tph
        );

        final long unitsConsumedBacklog = tphByDate.getOrDefault(entry.getKey(), 0L);
        final long unitsLeftOverBacklog = quantities.backlog() - unitsConsumedBacklog;

        final long unitsConsumedForecast = Math.min(tphForecast, quantities.forecast());
        final long unitsLeftOverForecast = quantities.forecast() - unitsConsumedForecast;

        remainingTph -= (unitsConsumedBacklog + unitsConsumedForecast);
        tphForecast -= unitsConsumedForecast;

        final Instant dateOut = entry.getKey();
        leftOver.put(dateOut, new BacklogAndForecastByDate.Quantity(unitsLeftOverBacklog, unitsLeftOverForecast));
        consumed.put(dateOut,
            new BacklogAndForecastByDate.QuantityWithEndDate(unitsConsumedBacklog, unitsConsumedForecast, processingEndDate));

      }
    }
    return new BacklogConsume(new BacklogAndForecastByDate(consumed), new BacklogAndForecastByDate(leftOver));

  }

  private Instant getProjectedEndDate(
      final Instant startingDate,
      final long inflectionPointSizeInSeconds,
      final long quantityToProcess,
      final long remainingProcessingPower,
      final long wholeHourPower
  ) {
    if (quantityToProcess <= remainingProcessingPower) {
      return startingDate.plusSeconds((inflectionPointSizeInSeconds * quantityToProcess) / wholeHourPower);
    }
    return null;
  }

  private Map<Instant, Long> getTphByDate(int tph, Map<Instant, Backlog> backlogs) {
    var totalBacklog = backlogs.values().stream()
        .map(BacklogAndForecastByDate.BacklogAndForecast.class::cast)
        .mapToLong(BacklogAndForecastByDate.BacklogAndForecast::backlog)
        .sum();
    var remainingTph = tph;

    Map<Instant, Long> tphByDate = new HashMap<>();

    for (Map.Entry<Instant, ? extends Backlog> entry : backlogs.entrySet()) {
      BacklogAndForecastByDate.BacklogAndForecast back = (BacklogAndForecastByDate.BacklogAndForecast) entry.getValue();
      double percentage = MathUtil.safeDiv(back.backlog(), totalBacklog);
      long assignTph = (long) (tph * percentage);

      tphByDate.put(entry.getKey(), assignTph);
      remainingTph -= assignTph;
    }

    while (remainingTph > 0 && totalBacklog > 0) {
      for (Map.Entry<Instant, Long> entry : tphByDate.entrySet()) {
        if (remainingTph > 0) {
          tphByDate.put(entry.getKey(), entry.getValue() + 1);
          remainingTph--;
        } else {
          break;
        }
      }
    }

    return tphByDate.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> Math.min(entry.getValue(), ((BacklogAndForecastByDate.BacklogAndForecast) backlogs.get(entry.getKey())).backlog())
        ));

  }
}
