package com.mercadolibre.planning.model.api.projection;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.flow.projection.tools.services.entities.context.Backlog;
import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.ProcessedBacklogState;
import com.mercadolibre.flow.projection.tools.services.entities.context.Upstream;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.builder.PackingProjectionBuilder;
import com.mercadolibre.planning.model.api.projection.builder.Projector;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProjectionTest {

  private static final Instant[] DATES = {
      Instant.parse("2023-09-04T00:00:00Z"),
      Instant.parse("2023-09-04T01:00:00Z"),
      Instant.parse("2023-09-04T02:00:00Z"),
      Instant.parse("2023-09-04T03:00:00Z"),
      Instant.parse("2023-09-04T04:00:00Z"),
      Instant.parse("2023-09-04T05:00:00Z"),
      Instant.parse("2023-09-04T06:00:00Z"),
  };

  private static final Instant[] SLAS = {
      Instant.parse("2023-09-04T10:00:00Z"),
      Instant.parse("2023-09-04T11:00:00Z"),
      Instant.parse("2023-09-04T12:00:00Z"),
      Instant.parse("2023-09-04T13:00:00Z"),
      Instant.parse("2023-09-04T14:00:00Z"),
  };
  private static final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> CURRENT_BACKLOGS = Map.of(
      PICKING, Map.of(
          TOT_MONO, Map.of(SLAS[0], 1000L, SLAS[1], 1000L),
          NON_TOT_MONO, Map.of(SLAS[0], 250L),
          TOT_MULTI_BATCH, Map.of(SLAS[1], 500L)
      ),
      PACKING, Map.of(
          TOT_MONO, Map.of(SLAS[0], 1000L, SLAS[2], 500L),
          NON_TOT_MONO, Map.of(SLAS[0], 100L)
      ),
      BATCH_SORTER, Map.of(
          TOT_MULTI_BATCH, Map.of(SLAS[0], 1000L, SLAS[2], 500L)
      ),
      WALL_IN, Map.of()
  );
  private static final Map<ProcessName, Map<Instant, Integer>> THROUGHPUT = Map.of(
      PICKING, throughputValues(),
      PACKING, throughputValues(),
      BATCH_SORTER, throughputValues(),
      WALL_IN, throughputValues(),
      PACKING_WALL, throughputValues()
  );
  private static final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> FORECAST_BACKLOG = Map.of(
      DATES[0], Map.of(
          TOT_MONO, Map.of(
              SLAS[0], 240L
          )
      ),
      DATES[1], Map.of(
          TOT_MULTI_BATCH, Map.of(
              SLAS[1], 1200L
          )
      ),
      DATES[5], Map.of(
          NON_TOT_MONO, Map.of(
              SLAS[4], 1800L
          )
      )
  );

  private static Map<Instant, Integer> throughputValues() {
    return Map.of(
        DATES[0], 1000,
        DATES[1], 1000,
        DATES[2], 1000,
        DATES[3], 1000,
        DATES[4], 1000,
        DATES[5], 1000,
        DATES[6], 1000
    );
  }

  private static Stream<Arguments> testArguments() {
    return Stream.of(
        Arguments.of(
            DATES[0],
            DATES[6],
            CURRENT_BACKLOGS,
            FORECAST_BACKLOG,
            THROUGHPUT,
            new PackingProjectionBuilder()
        ),
        Arguments.of(
            DATES[0],
            DATES[6],
            CURRENT_BACKLOGS,
            emptyMap(),
            THROUGHPUT,
            new PackingProjectionBuilder()
        ),
        Arguments.of(
            DATES[0],
            DATES[6],
            emptyMap(),
            emptyMap(),
            THROUGHPUT,
            new PackingProjectionBuilder()
        )
    );
  }

  @ParameterizedTest
  @MethodSource("testArguments")
  @DisplayName("Test projections")
  void check_projections(
      final Instant dateFrom,
      final Instant dateTo,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklogs,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput,
      final Projector projector
  ) {
    // GIVEN
    final Upstream upstream = projector.toUpstream(forecastBacklog);

    final ContextsHolder holder = projector.buildContextHolder(currentBacklogs, throughput);

    final Processor graph = projector.buildGraph();

    final List<Instant> inflectionPoints = DateUtils.generateInflectionPoints(DATES[0], DATES[6], 5);

    final ContextsHolder contextsHolderExpected = graph.accept(holder, upstream, inflectionPoints);

    // WHEN
    final ContextsHolder contextsHolder =
        Projection.execute(dateFrom, dateTo, currentBacklogs, forecastBacklog, throughput, projector);

    // THEN
    final List<Backlog> actual = contextsHolder.getProcessContextByProcessName().values().stream()
        .flatMap(processContext -> processContext.getProcessedBacklog().stream()
            .map(ProcessedBacklogState::getBacklog)
        )
        .collect(Collectors.toList());

    final List<Backlog> expected = contextsHolderExpected.getProcessContextByProcessName().values().stream()
        .flatMap(processContext -> processContext.getProcessedBacklog().stream()
            .map(ProcessedBacklogState::getBacklog)
        )
        .collect(Collectors.toList());

    assertNotNull(expected);
    assertNotNull(actual);
    assertEquals(expected, actual);
    assertEquals(expected.size(), actual.size());
  }
}
