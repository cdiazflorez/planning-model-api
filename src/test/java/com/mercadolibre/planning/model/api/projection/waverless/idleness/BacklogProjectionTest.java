package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.projection.waverless.idleness.BacklogProjection.buildContexts;
import static com.mercadolibre.planning.model.api.projection.waverless.idleness.BacklogProjection.buildGraph;
import static com.mercadolibre.planning.model.api.projection.waverless.idleness.BacklogProjection.project;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BacklogProjectionTest {

  private static final Instant[] DATES = {
      Instant.parse("2023-03-29T00:00:00Z"),
      Instant.parse("2023-03-29T01:00:00Z"),
      Instant.parse("2023-03-29T02:00:00Z"),
      Instant.parse("2023-03-29T03:00:00Z"),
      Instant.parse("2023-03-29T04:00:00Z"),
      Instant.parse("2023-03-29T05:00:00Z"),
      Instant.parse("2023-03-29T06:00:00Z"),
  };

  private static Map<Instant, Integer> throughput(int v1, int v2, int v3, int v4, int v5, int v6) {
    return Map.of(
        DATES[0], v1,
        DATES[1], v2,
        DATES[2], v3,
        DATES[3], v4,
        DATES[4], v5,
        DATES[5], v6,
        DATES[6], 1000
    );
  }

  private static void assertUnprocessedBacklogs(
      final List<BacklogQuantityAtInflectionPoint> listBacklogs, final ProcessName process, final List<Long> totals) {

    final List<BacklogQuantityAtInflectionPoint> listUnprocessedBacklog = listBacklogs.stream()
        .filter(backlog -> Objects.equals(backlog.getProcessName(), process))
        .collect(Collectors.toList());

    assertEquals(totals.size(), listUnprocessedBacklog.size());

    for (int i = 0; i < totals.size(); i++) {
      assertEquals(totals.get(i), listUnprocessedBacklog.get(i).getQuantity());
    }
  }


  @Test
  @DisplayName("on backlog projection, then backlog must flow through processes")
  void testBacklogProjection() {
    // GIVEN
    final PiecewiseUpstream upstream = new PiecewiseUpstream(emptyMap());

    final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklogs = Map.of(
        PICKING, Map.of(
            TOT_MONO, Map.of(DATES[0], 1000L, DATES[1], 1000L),
            NON_TOT_MONO, Map.of(DATES[0], 250L),
            TOT_MULTI_BATCH, Map.of(DATES[1], 500L)
        ),
        PACKING, Map.of(
            TOT_MONO, Map.of(DATES[0], 1000L, DATES[2], 500L),
            NON_TOT_MONO, Map.of(DATES[0], 100L)
        ),
        BATCH_SORTER, Map.of(
            TOT_MULTI_BATCH, Map.of(DATES[0], 1000L, DATES[2], 500L),
            NON_TOT_MULTI_BATCH, Map.of(DATES[1], 100L)
        ),
        WALL_IN, Map.of()
    );

    final Map<ProcessName, Map<Instant, Integer>> throughput = Map.of(
        PICKING, throughput(1000, 1000, 1000, 1000, 1000, 1000),
        PACKING, throughput(250, 500, 750, 0, 100, 1000),
        BATCH_SORTER, throughput(500, 500, 500, 500, 500, 500),
        WALL_IN, throughput(0, 0, 0, 0, 1000, 1000),
        PACKING_WALL, throughput(1000, 1000, 1000, 1000, 0, 500)
    );

    final ContextsHolder holder = buildContexts(currentBacklogs, throughput);

    final Processor graph = buildGraph();

    final List<Instant> inflectionPoints = Arrays.asList(DATES);

    final Set<ProcessName> processes = new HashSet<>(Arrays.asList(PICKING, PACKING, BATCH_SORTER, WALL_IN, PACKING_WALL));

    final List<BacklogQuantityAtInflectionPoint> listUnprocessedBacklog = project(graph, holder, upstream, inflectionPoints, processes);

    // THEN
    assertNotNull(listUnprocessedBacklog);
    assertFalse(listUnprocessedBacklog.isEmpty());

    // PICKING: 1752, 753, 0, 0, 0, 0
    assertUnprocessedBacklogs(listUnprocessedBacklog, PICKING, List.of(1752L, 753L, 0L, 0L, 0L, 0L));
    // PACKING: 1350, 1667, 1734, 2350, 2250, 1250
    assertUnprocessedBacklogs(listUnprocessedBacklog, PACKING, List.of(1350L, 1667L, 1734L, 2350L, 2250L, 1250L));
    // BATCH_SORTER: 1100, 781, 463, 100, 0, 0
    assertUnprocessedBacklogs(listUnprocessedBacklog, BATCH_SORTER, List.of(1100L, 781L, 463L, 100L, 0L, 0L));
    // WALL_IN: 0, 500, 1000, 1500, 1000, 100
    assertUnprocessedBacklogs(listUnprocessedBacklog, WALL_IN, List.of(0L, 500L, 1000L, 1500L, 1000L, 100L));
    // PACKING_WALL: 0, 0, 0, 0, 0, 500
    assertUnprocessedBacklogs(listUnprocessedBacklog, PACKING_WALL, List.of(0L, 0L, 0L, 0L, 0L, 500L));
  }

}
