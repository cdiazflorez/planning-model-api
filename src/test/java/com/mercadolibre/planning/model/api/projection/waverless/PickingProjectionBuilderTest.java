package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.process.ParallelProcess;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.flow.projection.tools.services.entities.process.SimpleProcess;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class PickingProjectionBuilderTest {

  private static final Instant DATE_1 = Instant.parse("2023-02-17T10:00:00Z");

  private static final Instant DATE_2 = Instant.parse("2023-02-17T11:00:00Z");

  private static final Instant DATE_OUT_1 = Instant.parse("2023-02-17T12:00:00Z");

  private static final Instant DATE_OUT_2 = Instant.parse("2023-02-17T13:00:00Z");

  private static final String PICKING_PROCESS = "picking";

  private static final List<ProcessPath> PROCESS_PATHS = List.of(TOT_MONO, NON_TOT_MONO);

  @Test
  @DisplayName("when building graph then return one processor for picking")
  void testGraphBuilding() {
    // GIVEN
    final var processPaths = List.of(TOT_MONO, TOT_MULTI_BATCH);

    // WHEN
    final var graph = PickingProjectionBuilder.buildGraph(processPaths);

    // THEN
    assertEquals(PICKING_PROCESS, graph.getName());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @DisplayName("when building graph without process paths then an exception should be thrown")
  void testGraphBuildingWithError(List<ProcessPath> processPaths) {
    // WHEN - THEN
    assertThrows(
        IllegalArgumentException.class, () -> PickingProjectionBuilder.buildGraph(processPaths)
    );
  }

  @Test
  @DisplayName("when building contexts then return one context per process path and one for picking")
  void testContextHolderBuilding() {
    // GIVEN
    final var currentBacklog = Map.of(
        TOT_MONO, Map.of(DATE_OUT_1, 100, DATE_OUT_2, 200),
        NON_TOT_MONO, Map.of(DATE_OUT_1, 200, DATE_OUT_2, 300)
    );

    final var throughput = Map.of(
        TOT_MONO, Map.of(DATE_1, 200, DATE_2, 500),
        NON_TOT_MONO, Map.of(DATE_1, 600, DATE_2, 600)
    );

    // WHEN
    final var holder = PickingProjectionBuilder.buildContextHolder(currentBacklog, throughput);

    // THEN
    assertEquals(3, holder.getProcessContextByProcessName().size());

    assertNotNull(holder.getProcessContextByProcessName(PICKING_PROCESS));
    assertNotNull(holder.getProcessContextByProcessName(TOT_MONO.toString()));
    assertNotNull(holder.getProcessContextByProcessName(NON_TOT_MONO.toString()));

    assertEquals(ParallelProcess.Context.class, holder.getProcessContextByProcessName(PICKING_PROCESS).getClass());
    assertEquals(SimpleProcess.Context.class, holder.getProcessContextByProcessName(TOT_MONO.toString()).getClass());
    assertEquals(SimpleProcess.Context.class, holder.getProcessContextByProcessName(NON_TOT_MONO.toString()).getClass());

    final var totMonoContext = (SimpleProcess.Context) holder.getProcessContextByProcessName(TOT_MONO.toString());
    assertEquals(300, totMonoContext.getInitialBacklog().total());
    assertEquals(200, totMonoContext.getTph().availableBetween(DATE_1, DATE_2));

    final var nonTotMonoContext = (SimpleProcess.Context) holder.getProcessContextByProcessName(NON_TOT_MONO.toString());
    assertEquals(500, nonTotMonoContext.getInitialBacklog().total());
    assertEquals(600, nonTotMonoContext.getTph().availableBetween(DATE_1, DATE_2));
  }

  @Test
  @DisplayName("when running a sla projection then return projected end date by process path and sla")
  void testSlaProjection() {
    // GIVEN
    final var currentBacklog = Map.of(
        TOT_MONO, Map.of(DATE_OUT_1, 100, DATE_OUT_2, 200),
        NON_TOT_MONO, Map.of(DATE_OUT_1, 200, DATE_OUT_2, 300)
    );

    final var throughput = Map.of(
        TOT_MONO, Map.of(DATE_1, 300, DATE_2, 500),
        NON_TOT_MONO, Map.of(DATE_1, 600, DATE_2, 600)
    );

    final var holder = PickingProjectionBuilder.buildContextHolder(currentBacklog, throughput);

    final var processPaths = List.of(TOT_MONO, NON_TOT_MONO);

    final var graph = PickingProjectionBuilder.buildGraph(processPaths);

    final var waves = Map.of(
        DATE_1, Map.of(TOT_MONO, Map.of(DATE_OUT_1, 100L)),
        DATE_2, Map.of(TOT_MONO, Map.of(DATE_OUT_1, 100L))
    );

    // WHEN
    final var projectedEndDates = PickingProjectionBuilder.projectSla(
        graph,
        holder,
        waves,
        List.of(DATE_1, DATE_2),
        processPaths,
        List.of(DATE_OUT_1, DATE_OUT_2)
    );

    // THEN
    assertNotNull(projectedEndDates);

    // [100 (current) + 100 (wave)] / 300 (tph) * 60 (min) = 40 min
    final Instant expectedDateOut1Result = Instant.parse("2023-02-17T10:40:00Z");
    final var totMonoProjections = projectedEndDates.get(TOT_MONO);
    assertNotNull(totMonoProjections);
    assertEquals(expectedDateOut1Result, totMonoProjections.get(DATE_OUT_1));
    assertNull(totMonoProjections.get(DATE_OUT_2));

    final Instant expectedDateOut2Result = Instant.parse("2023-02-17T10:20:00Z");
    final var nonTotMonoProjections = projectedEndDates.get(NON_TOT_MONO);
    assertNotNull(nonTotMonoProjections);
    assertEquals(expectedDateOut2Result, nonTotMonoProjections.get(DATE_OUT_1));
    assertNull(nonTotMonoProjections.get(DATE_OUT_2));
  }

  @Test
  @DisplayName("Backlog projection")
  void testBacklogProjection() {
    // GIVEN
    final var currentBacklog = Map.of(
        TOT_MONO, Map.of(DATE_OUT_1, 200, DATE_OUT_2, 100),
        NON_TOT_MONO, Map.of(DATE_OUT_1, 300, DATE_OUT_2, 400)
    );

    final var throughput = Map.of(
        TOT_MONO, Map.of(DATE_1, 250, DATE_2, 25),
        NON_TOT_MONO, Map.of(DATE_1, 600, DATE_2, 50)
    );

    final var upstream = new PiecewiseUpstream(Collections.emptyMap());

    final var holder = PickingProjectionBuilder.buildContextHolder(currentBacklog, throughput);

    final var ip = List.of(DATE_1, DATE_2, DATE_2.plus(1, ChronoUnit.HOURS));

    final var processPaths = Set.of(TOT_MONO, NON_TOT_MONO);
    //WHEN
    var backlogProjections = PickingProjectionBuilder.backlogProjection(graphMock(), holder, upstream, ip, processPaths);

    //THEN
    var expectedBacklog = expectedBacklogProjection();

    assertEquals(expectedBacklog.size(), backlogProjections.size());

    for (PickingProjectionBuilder.BacklogProjected backlog : backlogProjections) {
      assertTrue(expectedBacklog.stream().anyMatch(expected -> expected.equals(backlog)));
    }
  }

  Processor graphMock() {
    final List<Processor> processors = PROCESS_PATHS.stream()
        .map(pp -> new SimpleProcess(pp.toString()))
        .collect(Collectors.toList());

    return new ParallelProcess(PICKING_PROCESS, processors);
  }

  List<PickingProjectionBuilder.BacklogProjected> expectedBacklogProjection() {
    return List.of(
        new PickingProjectionBuilder.BacklogProjected(
            DATE_1,
            TOT_MONO,
            ProcessName.PICKING,
            DATE_OUT_1,
            0L
        ),
        new PickingProjectionBuilder.BacklogProjected(
            DATE_1,
            TOT_MONO,
            ProcessName.PICKING,
            DATE_OUT_2,
            50L
        ),
        new PickingProjectionBuilder.BacklogProjected(
            DATE_1,
            NON_TOT_MONO,
            ProcessName.PICKING,
            DATE_OUT_1,
            0L
        ),
        new PickingProjectionBuilder.BacklogProjected(
            DATE_1,
            NON_TOT_MONO,
            ProcessName.PICKING,
            DATE_OUT_2,
            100L
        ),
        new PickingProjectionBuilder.BacklogProjected(
            DATE_2,
            TOT_MONO,
            ProcessName.PICKING,
            DATE_OUT_2,
            25L
        ),
        new PickingProjectionBuilder.BacklogProjected(
            DATE_2,
            NON_TOT_MONO,
            ProcessName.PICKING,
            DATE_OUT_2,
            50L
        )

    );
  }

}
