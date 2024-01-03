package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_ORDER;
import static java.time.Instant.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BoundsCalculatorTest {

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

  private static final Map<ProcessPath, Map<Instant, Integer>> THROUGHPUT = Map.of(
      GLOBAL,
      MOCK_USELESS_THROUGHPUT,
      TOT_MONO, Map.of(
          HOUR_2, 300,
          HOUR_3, 500,
          HOUR_4, 800,
          HOUR_5, 60
      ),
      NON_TOT_MONO, Map.of(
          HOUR_2, 77,
          HOUR_3, 33,
          HOUR_4, 45,
          HOUR_5, 11
      ),
      TOT_MULTI_ORDER, Map.of(
          HOUR_2, 24,
          HOUR_3, 19,
          HOUR_4, 150,
          HOUR_5, 300
      ),
      TOT_MULTI_BATCH, Map.of(
          HOUR_2, 1000,
          HOUR_3, 0,
          HOUR_4, 0,
          HOUR_5, 112
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
        arguments("2023-04-12T03:10:00Z", 20, expected(166, 11, 6, 0)),
        arguments("2023-04-12T03:45:00Z", 30, expected(325, 19, 41, 0)),
        arguments("2023-04-12T02:10:00Z", 40, expected(200, 51, 16, 666)),
        arguments("2023-04-12T03:00:00Z", 50, expected(416, 27, 15, 0)),
        arguments("2023-04-12T02:45:00Z", 60, expected(450, 43, 20, 250)),
        arguments("2023-04-12T03:10:00Z", 70, expected(682, 42, 65, 0)),
        arguments("2023-04-12T03:45:00Z", 80, expected(930, 53, 179, 9)),
        arguments("2023-04-12T02:10:00Z", 90, expected(583, 86, 32, 833)),
        arguments("2023-04-12T03:00:00Z", 100, expected(1033, 63, 119, 0)),
        arguments("2023-04-12T02:45:00Z", 110, expected(1041, 78, 112, 250))
    );
  }

  @ParameterizedTest
  @MethodSource("parameters")
  void test(final String waveDate, final int minutes, final Map<ProcessPath, Integer> expected) {
    // GIVEN
    final var date = parse(waveDate);

    // WHEN
    final var actual = BoundsCalculator.execute(minutes, date, THROUGHPUT);

    // THEN
    assertEquals(expected, actual);
  }
}
