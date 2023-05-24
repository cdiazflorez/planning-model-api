package com.mercadolibre.planning.model.api.projection.backlogmanager;

import static java.util.Collections.emptyMap;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.Splitter;
import com.mercadolibre.flow.projection.tools.services.entities.context.Upstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.UpstreamAtInflectionPoint;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;

@Value
public class ProcessPathSplitter implements Splitter {

  private static final UpstreamAtInflectionPoint DEFAULT_UPSTREAM = new UpstreamAtInflectionPoint(emptyMap());

  Set<ProcessPath> processPaths;

  private static Stream<Tuple> mapOrderedBacklogToTuple(final Instant date, final Backlog backlog) {
    final var orderedBacklogs = (OrderedBacklogByProcessPath) backlog;
    return orderedBacklogs.getBacklogs()
        .entrySet()
        .stream()
        .map(backlogEntry -> new Tuple(date, backlogEntry.getKey(), backlogEntry.getValue()));
  }

  /**
   * Pivots the backlog representation from instant, process_path, backlog to
   * process_path, instant, backlog.
   *
   * <p>This split method only works with {@link OrderedBacklogByProcessPath} as it has to know the backlogs' internal representation.
   *
   * <p>If a process path is missing in the backlogToSplit it will be filled with an empty representation to avoid NPEs.
   *
   * @param upstream an amount of {@link Backlog} that must be divided
   * @return backlog by process path and instant
   */
  @Override
  public Map<String, Upstream> split(final Upstream upstream) {
    final var piecewiseUpstream = (UpstreamAtInflectionPoint) upstream;
    final var backlog = piecewiseUpstream.getUpstreamByPiece()
        .entrySet()
        .stream()
        .flatMap(u -> mapOrderedBacklogToTuple(u.getKey(), u.getValue()))
        .collect(
            Collectors.groupingBy(
                Tuple::getProcessPath,
                Collectors.collectingAndThen(
                    Collectors.toMap(Tuple::getDate, Tuple::getBacklog),
                    UpstreamAtInflectionPoint::new
                )
            )
        );

    return processPaths.stream()
        .collect(Collectors.toMap(
            ProcessPath::toString,
            pp -> backlog.getOrDefault(pp, DEFAULT_UPSTREAM)
        ));
  }

  @Value
  private static class Tuple {
    Instant date;

    ProcessPath processPath;

    Backlog backlog;
  }
}
