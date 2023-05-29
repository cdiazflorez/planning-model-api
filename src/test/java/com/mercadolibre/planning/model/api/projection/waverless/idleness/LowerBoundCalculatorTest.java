package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static java.time.Instant.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LowerBoundCalculatorTest {

  private static final Instant HOUR_2 = parse("2023-04-12T02:00:00Z");

  private static final Instant HOUR_3 = parse("2023-04-12T03:00:00Z");

  private static final Instant HOUR_4 = parse("2023-04-12T04:00:00Z");

  private static final Instant HOUR_5 = parse("2023-04-12T05:00:00Z");

  private static final Map<Instant, Integer> MOCK_USELESS_THROUGHPUT = Map.of(
      HOUR_2, 15000,
      HOUR_3, 15000,
      HOUR_4, 15000,
      HOUR_5, 15000
  );

  private static final Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> THROUGHPUT = Map.of(
      GLOBAL, Map.of(
          PICKING, MOCK_USELESS_THROUGHPUT,
          PACKING, MOCK_USELESS_THROUGHPUT
      ),
      TOT_MONO, Map.of(
          PICKING, Map.of(
              HOUR_2, 300,
              HOUR_3, 500,
              HOUR_4, 800,
              HOUR_5, 60
          ),
          PACKING, MOCK_USELESS_THROUGHPUT
      ),
      NON_TOT_MONO, Map.of(
          PICKING, Map.of(
              HOUR_2, 77,
              HOUR_3, 33,
              HOUR_4, 45,
              HOUR_5, 11
          ),
          PACKING, MOCK_USELESS_THROUGHPUT
      ),
      TOT_MULTI_ORDER, Map.of(
          PICKING, Map.of(
              HOUR_2, 24,
              HOUR_3, 19,
              HOUR_4, 150,
              HOUR_5, 300
          ),
          PACKING, MOCK_USELESS_THROUGHPUT
      ),
      TOT_MULTI_BATCH, Map.of(
          PICKING, Map.of(
              HOUR_2, 1000,
              HOUR_3, 0,
              HOUR_4, 0,
              HOUR_5, 112
          ),
          PACKING, MOCK_USELESS_THROUGHPUT
      )
  );

  private static Map<ProcessPath, Integer> expected(int totMono, int nonTotMono, int totMultiOrder, int totMultiBatch) {
    return Map.of(
        TOT_MONO, totMono,
        NON_TOT_MONO, nonTotMono,
        TOT_MULTI_ORDER, totMultiOrder,
        TOT_MULTI_BATCH, totMultiBatch
    );
  }

  static Stream<Arguments> parameters() {
    return Stream.of(
        arguments(named("minutes of the same hour", "2023-04-12T03:10:00Z"), 30, 0, expected(250, 16, 9, 0)),
        arguments(named("minutes of two hours", "2023-04-12T03:45:00Z"), 30, 0, expected(325, 19, 41, 0)),
        arguments(named("minutes of three hours", "2023-04-12T02:10:00Z"), 180, 0, expected(1560, 143, 239, 851)),
        arguments(named("exactly one hour", "2023-04-12T03:00:00Z"), 60, 0, expected(500, 33, 19, 0)),
        arguments(named("minutes out of bound", "2023-04-12T05:45:00Z"), 30, 0, expected(15, 2, 75, 28)),

        arguments(named("minutes of the same hour", "2023-04-12T03:10:00Z"), 30, 30, expected(277, 17, 10, 0)),
        arguments(named("minutes of two hours", "2023-04-12T03:45:00Z"), 30, 40, expected(361, 21, 42, 0)),
        arguments(named("minutes of three hours", "2023-04-12T02:10:00Z"), 180, 50, expected(1570, 145, 239, 886)),
        arguments(named("exactly one hour", "2023-04-12T03:00:00Z"), 60, 60, expected(554, 36, 21, 0)),
        arguments(named("minutes out of bound", "2023-04-12T05:45:00Z"), 30, 70, expected(23, 3, 118, 44))
    );
  }

  @ParameterizedTest
  @MethodSource("parameters")
  void test(final String waveDate, final int minutes, final int units, final Map<ProcessPath, Integer> expected) {
    // GIVEN
    final var date = parse(waveDate);

    // WHEN
    final var actual = LowerBoundCalculator.lowerBounds(minutes, units, date, THROUGHPUT);

    // THEN
    assertEquals(expected, actual);
  }

}
