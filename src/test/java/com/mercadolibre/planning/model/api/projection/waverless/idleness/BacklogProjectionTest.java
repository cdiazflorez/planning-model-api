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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.context.ProcessedBacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate;
import com.mercadolibre.flow.projection.tools.services.entities.orderedbacklogbydate.OrderedBacklogByDate.Quantity;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.waverless.OrderedBacklogByProcessPath;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private static void assertBacklogTotals(final ContextsHolder holder, final ProcessName process, final List<Long> totals) {
    final var context = holder.getProcessContextByProcessName(process.getName());
    final var processedBacklog = context.getProcessedBacklog();

    assertEquals(totals.size(), processedBacklog.size());

    for (int i = 0; i < totals.size(); i++) {
      assertEquals(totals.get(i), processedBacklog.get(i).getBacklog().total());
    }
  }

  private static void assertPickingProcessedBacklog(
      final ProcessedBacklogState processedBacklog,
      final ProcessPath processPath,
      final Long expected
  ) {
    final var actual = Optional.of(processedBacklog)
        .map(ProcessedBacklogState::getBacklog)
        .map(OrderedBacklogByProcessPath.class::cast)
        .map(OrderedBacklogByProcessPath::getBacklogs)
        .map(b -> b.get(processPath))
        .map(Backlog::total)
        .orElse(0L);

    assertEquals(expected, actual);
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

    // WHEN
    final var backlogs = project(graph, holder, upstream, inflectionPoints);

    // THEN
    assertNotNull(backlogs);

    // values should be 1000, 1000, 750 but differ because of rounding errors
    assertBacklogTotals(backlogs, PICKING, List.of(998L, 999L, 753L, 0L, 0L, 0L));
    assertBacklogTotals(backlogs, PACKING, List.of(250L, 500L, 750L, 0L, 100L, 1000L));
    assertBacklogTotals(backlogs, BATCH_SORTER, List.of(500L, 500L, 500L, 500L, 100L, 0L));
    assertBacklogTotals(backlogs, WALL_IN, List.of(0L, 0L, 0L, 0L, 1000L, 1000L));
    assertBacklogTotals(backlogs, PACKING_WALL, List.of(0L, 0L, 0L, 0L, 0L, 500L));
  }

  @Test
  @DisplayName("on backlog projection, then picking backlog must be split based on process path distribution")
  void testBacklogProjectionPickingDownstream() {
    // GIVEN
    final PiecewiseUpstream upstream = new PiecewiseUpstream(
        Map.of(
            DATES[1], new OrderedBacklogByProcessPath(
                Map.of(TOT_MULTI_BATCH, new OrderedBacklogByDate(Map.of(DATES[0], new Quantity(1002L))))
            ),
            DATES[2], new OrderedBacklogByProcessPath(
                Map.of(TOT_MULTI_BATCH, new OrderedBacklogByDate(Map.of(DATES[0], new Quantity(1000L))))
            ),
            DATES[3], new OrderedBacklogByProcessPath(
                Map.of(TOT_MULTI_BATCH, new OrderedBacklogByDate(Map.of(DATES[0], new Quantity(1000L))))
            )
        )
    );

    final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklogs = Map.of(
        PICKING, Map.of(
            TOT_MONO, Map.of(DATES[0], 1000L, DATES[1], 1000L),
            NON_TOT_MONO, Map.of(DATES[0], 1000L)

        )
    );

    final Map<ProcessName, Map<Instant, Integer>> throughput = Map.of(
        PICKING, throughput(1000, 1000, 1000, 1000, 1000, 1000),
        PACKING, throughput(1000, 1000, 1000, 1000, 1000, 1000),
        BATCH_SORTER, throughput(1000, 1000, 1000, 1000, 1000, 1000),
        WALL_IN, throughput(1000, 1000, 1000, 1000, 1000, 1000),
        PACKING_WALL, throughput(1000, 1000, 1000, 1000, 1000, 1000)
    );

    final ContextsHolder holder = buildContexts(currentBacklogs, throughput);

    final Processor graph = buildGraph();

    final List<Instant> inflectionPoints = Arrays.asList(DATES);

    // WHEN
    final var backlogs = project(graph, holder, upstream, inflectionPoints);

    // THEN
    assertNotNull(backlogs);

    final var pickingProcessedBacklog = backlogs.getProcessContextByProcessName(PICKING.getName()).getProcessedBacklog();
    assertPickingProcessedBacklog(pickingProcessedBacklog.get(0), TOT_MONO, 666L);
    assertPickingProcessedBacklog(pickingProcessedBacklog.get(0), NON_TOT_MONO, 333L);
    assertPickingProcessedBacklog(pickingProcessedBacklog.get(0), TOT_MULTI_BATCH, 0L);

    assertPickingProcessedBacklog(pickingProcessedBacklog.get(1), TOT_MONO, 444L);
    assertPickingProcessedBacklog(pickingProcessedBacklog.get(1), NON_TOT_MONO, 222L);
    assertPickingProcessedBacklog(pickingProcessedBacklog.get(1), TOT_MULTI_BATCH, 333L);

    assertPickingProcessedBacklog(pickingProcessedBacklog.get(2), TOT_MONO, 296L);
    assertPickingProcessedBacklog(pickingProcessedBacklog.get(2), NON_TOT_MONO, 148L);
    assertPickingProcessedBacklog(pickingProcessedBacklog.get(2), TOT_MULTI_BATCH, 555L);
  }

}
