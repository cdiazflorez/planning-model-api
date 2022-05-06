package com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.helper;

import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.BacklogHelper;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionService.ProcessedBacklog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.input.BacklogBySla;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.QuantityAtDate;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.output.SimpleProcessedBacklog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Backlog by Cpt Helper. Implements methods needed for projection.
 */
public class BacklogBySlaHelper implements BacklogHelper<BacklogBySla> {

  @Override
  public ProcessedBacklog<BacklogBySla> consume(final BacklogBySla from, final int units) {
    final List<QuantityAtDate> sortedBacklog = from.getDistributions()
        .stream()
        .sorted(Comparator.comparing(QuantityAtDate::getDate))
        .collect(Collectors.toList());

    final List<QuantityAtDate> processed = new ArrayList<>();
    final List<QuantityAtDate> carryOver = new ArrayList<>();

    int remainingUnits = units;
    for (QuantityAtDate backlog : sortedBacklog) {
      if (backlog.getQuantity() != 0) {
        final int proc = Math.min(remainingUnits, backlog.getQuantity());
        processed.add(new QuantityAtDate(backlog.getDate(), proc));

        final int carry = backlog.getQuantity() - proc;
        carryOver.add(new QuantityAtDate(backlog.getDate(), carry));

        remainingUnits = remainingUnits - proc;
      }
    }

    return new SimpleProcessedBacklog<>(
        new BacklogBySla(processed),
        new BacklogBySla(carryOver)
    );
  }

  @Override
  public BacklogBySla merge(final BacklogBySla one, final BacklogBySla other) {
    final List<QuantityAtDate> otherDistributions = other.getDistributions();

    final var mergedDistributions = Stream.concat(
            one.getDistributions().stream(),
            otherDistributions.stream()
        ).collect(
            Collectors.toMap(
                QuantityAtDate::getDate,
                QuantityAtDate::getQuantity,
                Integer::sum
            )
        )
        .entrySet()
        .stream()
        .map(entry -> new QuantityAtDate(entry.getKey(), entry.getValue()))
        .sorted(Comparator.comparing(QuantityAtDate::getDate))
        .collect(Collectors.toList());

    return new BacklogBySla(mergedDistributions);
  }
}
