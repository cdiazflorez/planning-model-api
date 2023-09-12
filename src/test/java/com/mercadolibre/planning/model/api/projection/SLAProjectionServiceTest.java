package com.mercadolibre.planning.model.api.projection;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.projection.builder.SlaProjectionResult.Sla;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.builder.PackingProjectionBuilder;
import com.mercadolibre.planning.model.api.projection.builder.Projector;
import com.mercadolibre.planning.model.api.projection.builder.SlaProjectionResult;
import java.time.Instant;
import java.util.Comparator;
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
public class SLAProjectionServiceTest {

  private static final Instant SLAS_1 = Instant.parse("2023-09-08T10:00:00Z");
  private static final Instant SLAS_2 = Instant.parse("2023-09-08T11:00:00Z");
  private static final Instant SLAS_3 = Instant.parse("2023-09-08T12:00:00Z");
  private static final Instant SLAS_4 = Instant.parse("2023-09-08T13:00:00Z");
  private static final Instant SLAS_5 = Instant.parse("2023-09-08T14:00:00Z");
  private static final Instant DATE_1 = Instant.parse("2023-09-08T00:00:00Z");
  private static final Instant DATE_2 = Instant.parse("2023-09-08T01:00:00Z");
  private static final Instant DATE_3 = Instant.parse("2023-09-08T02:00:00Z");
  private static final Instant DATE_4 = Instant.parse("2023-09-08T03:00:00Z");
  private static final Instant DATE_5 = Instant.parse("2023-09-08T04:00:00Z");
  private static final Instant DATE_6 = Instant.parse("2023-09-08T05:00:00Z");
  private static final Instant DATE_7 = Instant.parse("2023-09-08T06:00:00Z");

  private static final SlaProjectionResult EXPECTED_1 = new SlaProjectionResult(
      List.of(
          new Sla(SLAS_1, Instant.parse("2023-09-08T04:20:07Z"), 0D),
          new Sla(SLAS_2, Instant.parse("2023-09-08T04:31:01Z"), 0D),
          new Sla(SLAS_3, Instant.parse("2023-09-08T02:42:13Z"), 0D),
          new Sla(SLAS_4, Instant.parse("2023-09-08T03:40:32Z"), 0D),
          new Sla(SLAS_5, null, 0D)
      )
  );
  private static final SlaProjectionResult EXPECTED_2 = new SlaProjectionResult(
      List.of(
          new Sla(SLAS_1, Instant.parse("2023-09-08T02:50:18Z"), 0D),
          new Sla(SLAS_2, Instant.parse("2023-09-08T03:00:32Z"), 0D),
          new Sla(SLAS_3, Instant.parse("2023-09-08T03:15:46Z"), 0D),
          new Sla(SLAS_4, Instant.parse("2023-09-08T03:45:54Z"), 0D),
          new Sla(SLAS_5, Instant.parse("2023-09-08T03:51:55Z"), 0D)
      )
  );
  private static final SlaProjectionResult EXPECTED_3 = new SlaProjectionResult(List.of());

  private static final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> CURRENT_BACKLOGS = Map.of(
      PICKING, Map.of(
          TOT_MONO, Map.of(SLAS_1, 1000L, SLAS_2, 1000L),
          NON_TOT_MONO, Map.of(SLAS_1, 250L),
          TOT_MULTI_BATCH, Map.of(SLAS_2, 500L)
      ),
      PACKING, Map.of(
          TOT_MONO, Map.of(SLAS_3, 1000L, SLAS_4, 500L),
          NON_TOT_MONO, Map.of(SLAS_5, 100L)
      ),
      BATCH_SORTER, Map.of(
          TOT_MULTI_BATCH, Map.of(SLAS_1, 1000L, SLAS_3, 500L)
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
      DATE_1, Map.of(
          TOT_MONO, Map.of(
              SLAS_1, 240L
          )
      ),
      DATE_2, Map.of(
          TOT_MULTI_BATCH, Map.of(
              SLAS_2, 1200L
          )
      ),
      DATE_6, Map.of(
          NON_TOT_MONO, Map.of(
              SLAS_5, 1800L
          )
      )
  );

  private static Map<Instant, Integer> throughputValues() {
    return Map.of(
        DATE_1, 1000,
        DATE_2, 1000,
        DATE_3, 1000,
        DATE_4, 1000,
        DATE_5, 1000,
        DATE_6, 1000,
        DATE_7, 1000
    );
  }

  private static Stream<Arguments> testArguments() {
    return Stream.of(
        Arguments.of(
            DATE_1,
            DATE_7,
            CURRENT_BACKLOGS,
            FORECAST_BACKLOG,
            THROUGHPUT,
            new PackingProjectionBuilder(),
            EXPECTED_1
        ),
        Arguments.of(
            DATE_1,
            DATE_7,
            CURRENT_BACKLOGS,
            emptyMap(),
            THROUGHPUT,
            new PackingProjectionBuilder(),
            EXPECTED_2
        ),
        Arguments.of(
            DATE_1,
            DATE_7,
            emptyMap(),
            emptyMap(),
            THROUGHPUT,
            new PackingProjectionBuilder(),
            EXPECTED_3
        )
    );
  }

  @ParameterizedTest
  @MethodSource("testArguments")
  @DisplayName("Test SLAProjectionService")
  void check_projections(
      final Instant dateFrom,
      final Instant dateTo,
      final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> currentBacklogs,
      final Map<Instant, Map<ProcessPath, Map<Instant, Long>>> forecastBacklog,
      final Map<ProcessName, Map<Instant, Integer>> throughput,
      final Projector projector,
      final SlaProjectionResult expected
  ) {
    // WHEN
    final SlaProjectionResult actual =
        SLAProjectionService.execute(dateFrom, dateTo, currentBacklogs, forecastBacklog, throughput, emptyMap(), projector);

    // THEN
    assertNotNull(expected);
    assertNotNull(actual);
    assertEquals(expected.slas().size(), actual.slas().size());

    final List<Sla> slasExpected = expected.slas().stream()
        .sorted(Comparator.comparing(Sla::date))
        .toList();

    final List<Sla> slasActual = actual.slas().stream()
        .sorted(Comparator.comparing(Sla::date))
        .toList();

    assertEquals(slasExpected, slasActual);
  }
}
