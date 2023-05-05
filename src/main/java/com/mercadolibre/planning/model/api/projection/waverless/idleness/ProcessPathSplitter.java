package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_SINGLE_SKU;
import static com.mercadolibre.planning.model.api.projection.waverless.idleness.BacklogProjection.CONSOLIDATION_PROCESS_GROUP;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.Splitter;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.waverless.OrderedBacklogByProcessPath;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProcessPathSplitter implements Splitter {

  private static final Set<ProcessPath> PACKING_WALL_PROCESS_PATH = Set.of(TOT_MULTI_BATCH, NON_TOT_MULTI_BATCH);

  private static final Set<ProcessPath> PACKING_TOTE_PROCESS_PATH =
      Set.of(TOT_SINGLE_SKU, TOT_MONO, NON_TOT_MONO, TOT_MULTI_ORDER, NON_TOT_MULTI_ORDER);

  private final Function<Map<ProcessPath, Backlog>, Backlog> finisher;

  /**
   * Splits a PiecewiseUpstream of {@link OrderedBacklogByProcessPath} type between packing and consolidation steps
   * based on the process paths. All the unrecognized Process Paths will not be assigned to a downstream.
   *
   * <p>After being grouped, the final representation of the Backlog is obtained by invoking its finisher function. With this approach
   * any projection that uses OrderedBacklogByProcessPath as the main representation for Pickings' backlog can use this splitter
   * independently of the representation of the following Processes.
   *
   * @param backlogToSplit an amount of {@link Backlog} that must be divided
   * @return PiecewiseUpstream for packing and consolidation.
   */
  @Override
  public Map<String, PiecewiseUpstream> split(final PiecewiseUpstream backlogToSplit) {
    final var backlog = backlogToSplit.getUpstreamByPiece();

    return Map.of(
        PACKING.getName(), new PiecewiseUpstream(splitByProcessPath(backlog, PACKING_TOTE_PROCESS_PATH)),
        CONSOLIDATION_PROCESS_GROUP, new PiecewiseUpstream(splitByProcessPath(backlog, PACKING_WALL_PROCESS_PATH))
    );
  }

  private Map<Instant, Backlog> splitByProcessPath(final Map<Instant, Backlog> backlogs, final Set<ProcessPath> acceptedPaths) {
    return backlogs.entrySet()
        .stream()
        .collect(
            Collectors.toMap(Map.Entry::getKey, entry -> filterBacklogByProcessPath(entry.getValue(), acceptedPaths))
        );
  }

  private Backlog filterBacklogByProcessPath(final Backlog backlog, final Set<ProcessPath> acceptedPaths) {
    final var backlogByProcessPath = (OrderedBacklogByProcessPath) backlog;
    final var filteredBacklog = backlogByProcessPath.getBacklogs().entrySet()
        .stream()
        .filter(entry -> acceptedPaths.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    return finisher.apply(filteredBacklog);
  }
}
