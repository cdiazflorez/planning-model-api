package com.mercadolibre.planning.model.api.projection.builder;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.flow.projection.tools.services.entities.context.ContextsHolder;
import com.mercadolibre.flow.projection.tools.services.entities.context.PiecewiseUpstream;
import com.mercadolibre.flow.projection.tools.services.entities.process.Processor;
import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PackingProjectionBuilderTest {
  private static final Instant[] DATES = {
      Instant.parse("2023-03-29T00:00:00Z"),
      Instant.parse("2023-03-29T01:00:00Z"),
      Instant.parse("2023-03-29T02:00:00Z"),
      Instant.parse("2023-03-29T03:00:00Z"),
      Instant.parse("2023-03-29T04:00:00Z"),
      Instant.parse("2023-03-29T05:00:00Z"),
      Instant.parse("2023-03-29T06:00:00Z"),
  };

  private static final Instant[] SLAS = {
      Instant.parse("2023-03-29T10:00:00Z"),
      Instant.parse("2023-03-29T11:00:00Z"),
      Instant.parse("2023-03-29T12:00:00Z"),
      Instant.parse("2023-03-29T13:00:00Z"),
      Instant.parse("2023-03-29T14:00:00Z"),
  };

  private static final Map<Instant, Instant> EXPECTED_END_DATES_WITH_UPSTREAM = Map.of(
      SLAS[0], Instant.parse("2023-03-29T04:20:00Z"),
      SLAS[1], Instant.parse("2023-03-29T04:31:00Z"),
      SLAS[2], Instant.parse("2023-03-29T03:51:00Z")
  );

  private static final Map<Instant, Instant> EXPECTED_END_DATES_WITHOUT_UPSTREAM = Map.of(
      SLAS[0], Instant.parse("2023-03-29T02:50:00Z"),
      SLAS[1], Instant.parse("2023-03-29T03:21:00Z"),
      SLAS[2], Instant.parse("2023-03-29T03:51:00Z")
  );

  public static Map<Instant, Integer> throughput(int v1, int v2, int v3, int v4, int v5, int v6) {
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

  static Stream<Arguments> params() {
    return Stream.of(
        Arguments.of(
            Map.of(
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
            ),
            EXPECTED_END_DATES_WITH_UPSTREAM
        ),
        Arguments.of(
            Collections.emptyMap(),
            EXPECTED_END_DATES_WITHOUT_UPSTREAM
        )
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  @DisplayName("on backlog projection, then backlog must flow through processes")
  void testBacklogProjection(final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecast, final Map<Instant, Instant> expected) {
    // GIVEN
    final var builder = new PackingProjectionBuilder();

    final PiecewiseUpstream upstream = builder.toUpstream(forecast);

    final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklogs = Map.of(
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

    final Map<ProcessName, Map<Instant, Integer>> throughput = Map.of(
        PICKING, throughput(1000, 1000, 1000, 1000, 1000, 1000),
        PACKING, throughput(1000, 1000, 1000, 1000, 1000, 1000),
        BATCH_SORTER, throughput(1000, 1000, 1000, 1000, 1000, 1000),
        WALL_IN, throughput(1000, 1000, 1000, 1000, 1000, 1000),
        PACKING_WALL, throughput(1000, 1000, 1000, 1000, 1000, 1000)
    );

    final ContextsHolder holder = builder.buildContextHolder(currentBacklogs, throughput);

    final Processor graph = builder.buildGraph();

    final List<Instant> inflectionPoints = DateUtils.generateInflectionPoints(DATES[0], DATES[6], 5);

    final var updatedContext = graph.accept(holder, upstream, inflectionPoints);

    // WHEN
    final var result = builder.calculateProjectedEndDate(Arrays.asList(SLAS), updatedContext);

    // THEN
    assertNotNull(result);

    final var projectedEndDateBySla = result.slas()
        .stream()
        .filter(r -> r.projectedEndDate() != null)
        .collect(
            Collectors.toMap(
                SlaProjectionResult.Sla::date,
                r -> r.projectedEndDate().truncatedTo(ChronoUnit.MINUTES)
            )
        );


    for (final Instant sla : SLAS) {
      assertEquals(expected.get(sla), projectedEndDateBySla.get(sla));
    }
  }
}
