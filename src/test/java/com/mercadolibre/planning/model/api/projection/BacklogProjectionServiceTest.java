package com.mercadolibre.planning.model.api.projection;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.HU_ASSEMBLY;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.SALES_DISPATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static java.util.Collections.emptyMap;
import static org.springframework.test.util.AssertionErrors.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.builder.OutboundProjectionBuilder;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BacklogProjectionServiceTest {
  private static final Instant EXECUTION_DATE_FROM = Instant.parse("2023-03-29T00:00:00Z");

  private static final Instant EXECUTION_DATE_TO = Instant.parse("2023-03-29T06:00:00Z");

  private static final Instant[] DATES = {
      EXECUTION_DATE_FROM,
      Instant.parse("2023-03-29T01:00:00Z"),
      Instant.parse("2023-03-29T02:00:00Z"),
      Instant.parse("2023-03-29T03:00:00Z"),
      Instant.parse("2023-03-29T04:00:00Z"),
      Instant.parse("2023-03-29T05:00:00Z"),
      EXECUTION_DATE_TO,
  };

  private static final Map<Instant, Map<ProcessName, Map<Instant, Integer>>> EXPECTED_BACKLOG =
      Map.of(Instant.parse("2023-03-29T01:00:00Z"), Map.of(
          PACKING, Map.of(DATES[0], 3193),
          SALES_DISPATCH, Map.of(DATES[0], 430),
          PICKING, Map.of(DATES[0], 2418),
          BATCH_SORTER, Map.of(DATES[0], 1315),
          HU_ASSEMBLY, Map.of(DATES[0], 3233),
          PACKING_WALL, Map.of(DATES[0], 670),
          WALL_IN, Map.of(DATES[0], 360)
      ));

  private static Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentShippingBacklogs() {
    return Map.of(
        WAVING, Map.of(
            TOT_MONO, Map.of(DATES[0], 1L),
            NON_TOT_MONO, Map.of(DATES[0], 1L),
            TOT_MULTI_BATCH, Map.of(DATES[0], 1L)
        ),
        PICKING, Map.of(
            TOT_MONO, Map.of(DATES[0], 2500L),
            NON_TOT_MONO, Map.of(DATES[0], 1500L),
            TOT_MULTI_BATCH, Map.of(DATES[0], 2000L)
        ),
        PACKING, Map.of(
            TOT_MONO, Map.of(DATES[0], 3000L),
            NON_TOT_MONO, Map.of(DATES[0], 800L)
        ),
        BATCH_SORTER, Map.of(
            TOT_MULTI_BATCH, Map.of(DATES[0], 1780L)
        ),
        WALL_IN, Map.of(
            TOT_MULTI_BATCH, Map.of(DATES[0], 490L)

        ),
        PACKING_WALL, Map.of(
            TOT_MULTI_BATCH, Map.of(DATES[0], 800L)

        ),
        HU_ASSEMBLY, Map.of(
            TOT_MULTI_BATCH, Map.of(DATES[0], 800L)

        ),
        SALES_DISPATCH, Map.of(
            TOT_MULTI_BATCH, Map.of(DATES[0], 800L)
        )
    );
  }

  private static Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> noPickingBacklogs() {
    return Map.of(
        WAVING, Map.of(
            TOT_MONO, Map.of(DATES[0], 1L),
            NON_TOT_MONO, Map.of(DATES[0], 1L),
            TOT_MULTI_BATCH, Map.of(DATES[0], 1L)
        ),
        PACKING, Map.of(
            TOT_MONO, Map.of(DATES[0], 3000L),
            NON_TOT_MONO, Map.of(DATES[0], 800L)
        ),
        BATCH_SORTER, Map.of(
            TOT_MULTI_BATCH, Map.of(DATES[0], 1780L)
        ),
        WALL_IN, Map.of(
            TOT_MULTI_BATCH, Map.of(DATES[0], 490L)

        ),
        PACKING_WALL, Map.of(
            TOT_MULTI_BATCH, Map.of(DATES[0], 800L)

        ),
        HU_ASSEMBLY, Map.of(
            TOT_MULTI_BATCH, Map.of(DATES[0], 800L)

        ),
        SALES_DISPATCH, Map.of(
            TOT_MULTI_BATCH, Map.of(DATES[0], 800L)
        )
    );
  }

  private static Map<ProcessName, Map<Instant, Integer>> getShippingThroughput() {
    return Map.of(
        PICKING, throughput(3600, 3600, 3600, 3600, 3600, 3600),
        PACKING, throughput(2796, 2796, 2796, 2796, 2796, 2796),
        BATCH_SORTER, throughput(1560, 1560, 1560, 1560, 1560, 1560),
        WALL_IN, throughput(1560, 1560, 1560, 1560, 1560, 1560),
        PACKING_WALL, throughput(1560, 1560, 1560, 1560, 1560, 1560),
        HU_ASSEMBLY, throughput(1560, 1560, 1560, 1560, 1560, 1560),
        SALES_DISPATCH, throughput(1800, 1800, 1800, 1800, 1800, 1800)
    );
  }

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

  private static Stream<Arguments> testArguments() {
    return Stream.of(
        Arguments.of(
            EXECUTION_DATE_FROM,
            EXECUTION_DATE_TO,
            currentShippingBacklogs(),
            emptyMap(),
            getShippingThroughput(),
            EXPECTED_BACKLOG
        ),
        Arguments.of(
            EXECUTION_DATE_FROM,
            EXECUTION_DATE_TO,
            noPickingBacklogs(),
            emptyMap(),
            getShippingThroughput(),
            emptyMap()
        ),
        Arguments.of(
            EXECUTION_DATE_FROM,
            EXECUTION_DATE_TO,
            emptyMap(),
            emptyMap(),
            getShippingThroughput(),
            emptyMap()
        )
    );
  }

  @ParameterizedTest
  @MethodSource("testArguments")
  @DisplayName("Test shipping projections")
  void check_shipping_projections_atExactTime(
      final Instant dateFrom,
      final Instant dateTo,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentShippingBacklogs,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> upstream,
      final Map<ProcessName, Map<Instant, Integer>> getShippingThroughput,
      final Map<Instant, Map<ProcessName, Map<Instant, Integer>>> expected
  ) {
    final var projector = new OutboundProjectionBuilder();
    final var processes = List.of(PICKING, BATCH_SORTER, WALL_IN, PACKING, PACKING_WALL, HU_ASSEMBLY, SALES_DISPATCH);
    final Map<Instant, Map<ProcessName, Map<Instant, Integer>>> projectedBacklogs = BacklogProjectionService.execute(
        dateFrom,
        dateTo,
        currentShippingBacklogs,
        upstream,
        getShippingThroughput,
        processes,
        projector
    );
    var resultBacklogAtFirstHour = projectedBacklogs.get(Instant.parse("2023-03-29T01:00:00Z"));
    expected.forEach((key, value) -> value.forEach(
        (b, c) -> assertEquals("", c, resultBacklogAtFirstHour.get(b))
    ));
  }
}
