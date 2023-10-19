package com.mercadolibre.planning.model.api.projection.availablecapacity;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ThroughputCalculatorTest {

  private static final Instant DATE = Instant.parse("2023-10-09T10:00:00Z");

  private static final Instant DATE1 = Instant.parse("2023-10-09T11:00:00Z");

  private static final Instant DATE2 = Instant.parse("2023-10-09T12:00:00Z");

  private static final Instant DATE3 = Instant.parse("2023-10-09T13:00:00Z");

  private static final Instant DATE4 = Instant.parse("2023-10-09T14:00:00Z");

  private static final Map<Instant, Integer>[] TPH_BY_HOUR = new HashMap[9];

  static {
    TPH_BY_HOUR[0] = new HashMap<>();
    TPH_BY_HOUR[0].put(DATE, 100);

    TPH_BY_HOUR[1] = new HashMap<>();
    TPH_BY_HOUR[1].put(DATE, 200);

    TPH_BY_HOUR[2] = new HashMap<>();
    TPH_BY_HOUR[2].put(DATE, 300);

    TPH_BY_HOUR[3] = new HashMap<>();
    TPH_BY_HOUR[3].put(DATE1, 110);

    TPH_BY_HOUR[4] = new HashMap<>();
    TPH_BY_HOUR[4].put(DATE1, 120);

    TPH_BY_HOUR[5] = new HashMap<>();
    TPH_BY_HOUR[5].put(DATE1, 230);

    TPH_BY_HOUR[6] = new HashMap<>();
    TPH_BY_HOUR[6].put(DATE2, 200);
    TPH_BY_HOUR[6].put(DATE3, 200);
    TPH_BY_HOUR[6].put(DATE4, 200);

    TPH_BY_HOUR[7] = new HashMap<>();
    TPH_BY_HOUR[7].put(DATE2, 100);
    TPH_BY_HOUR[7].put(DATE3, 200);
    TPH_BY_HOUR[7].put(DATE4, 300);

    TPH_BY_HOUR[8] = new HashMap<>();
    TPH_BY_HOUR[8].put(DATE2, 200);
    TPH_BY_HOUR[8].put(DATE3, 50);
    TPH_BY_HOUR[8].put(DATE4, 100);
  }

  private static Stream<Arguments> testCases() {
    final Map<Instant, Integer> tphMap = Map.of(
        Instant.parse("2023-09-08T10:00:00Z"), 1000,
        Instant.parse("2023-09-08T11:00:00Z"), 500,
        Instant.parse("2023-09-08T12:00:00Z"), 800
    );

    final Instant from1 = Instant.parse("2023-09-08T09:00:00Z");
    final Instant to1 = Instant.parse("2023-09-08T13:00:00Z");

    final Instant from2 = Instant.parse("2023-09-08T10:30:00Z");
    final Instant to2 = Instant.parse("2023-09-08T11:30:00Z");

    final Instant from3 = Instant.parse("2023-09-08T08:00:00Z");
    final Instant to3 = Instant.parse("2023-09-08T09:00:00Z");

    final Instant from4 = Instant.parse("2023-09-08T12:30:00Z");
    final Instant to4 = Instant.parse("2023-09-08T13:30:00Z");

    final Instant from5 = Instant.parse("2023-09-08T09:00:00Z");
    final Instant to5 = Instant.parse("2023-09-08T10:30:00Z");

    final Instant from6 = Instant.parse("2023-09-08T13:00:00Z");
    final Instant to6 = Instant.parse("2023-09-08T14:00:00Z");

    final Instant from7 = Instant.parse("2023-09-08T10:30:00Z");
    final Instant to7 = Instant.parse("2023-09-08T12:00:00Z");

    final Instant from8 = Instant.parse("2023-09-08T10:00:00Z");
    final Instant to8 = Instant.parse("2023-09-08T11:30:00Z");

    return Stream.of(
        Arguments.of(from1, to1, tphMap, 2300),
        Arguments.of(from2, to2, tphMap, 750),
        Arguments.of(from3, to3, tphMap, 0),
        Arguments.of(from4, to4, tphMap, 400),
        Arguments.of(from5, to5, tphMap, 500),
        Arguments.of(from6, to6, tphMap, 0),
        Arguments.of(from7, to7, tphMap, 1000),
        Arguments.of(from8, to8, tphMap, 1250)
    );
  }

  private static Stream<Arguments> testCasesHourlyThroughputMinimum() {
    return Stream.of(
        Arguments.of(
            Map.of(
                WAVING, TPH_BY_HOUR[0],
                PICKING, TPH_BY_HOUR[2],
                PACKING, TPH_BY_HOUR[3],
                PACKING_WALL, TPH_BY_HOUR[4],
                BATCH_SORTER, TPH_BY_HOUR[0],
                WALL_IN, TPH_BY_HOUR[0]
            ),
            Map.of(
                DATE, 300,
                DATE1, 230
            )
        ),
        Arguments.of(
            Map.of(
                WAVING, TPH_BY_HOUR[1],
                PICKING, TPH_BY_HOUR[2],
                PACKING, TPH_BY_HOUR[0],
                BATCH_SORTER, TPH_BY_HOUR[2]
            ),
            TPH_BY_HOUR[0]
        ),
        Arguments.of(
            Map.of(
                WAVING, TPH_BY_HOUR[1],
                PICKING, TPH_BY_HOUR[1],
                PACKING, TPH_BY_HOUR[1]
            ),
            TPH_BY_HOUR[1]
        ),
        Arguments.of(
            Map.of(
                WAVING, TPH_BY_HOUR[6],
                PICKING, TPH_BY_HOUR[7],
                PACKING, TPH_BY_HOUR[8],
                PACKING_WALL, TPH_BY_HOUR[8]
            ),
            Map.of(
                DATE2, 100,
                DATE3, 100,
                DATE4, 200
            )
        )
    );
  }

  /**
   * Test to calculate the sum of values within a specified range.
   *
   * @param inputMap       The input map of timestamps and values.
   * @param from           The starting timestamp of the range.
   * @param to             The ending timestamp of the range.
   * @param expectedOutput The expected sum of values within the range.
   */
  @ParameterizedTest
  @MethodSource("testCases")
  void testTotalWithinRange(
      final Instant from,
      final Instant to,
      final Map<Instant, Integer> inputMap,
      final int expectedOutput
  ) {
    final int actualSum = ThroughputCalculator.totalWithinRange(inputMap, from, to);
    assertEquals(expectedOutput, actualSum);
  }

  @ParameterizedTest
  @MethodSource("testCasesHourlyThroughputMinimum")
  void testHourlyThroughputMinimum(
      final Map<ProcessName, Map<Instant, Integer>> throughputByProcess,
      final Map<Instant, Integer> expected) {

    final var response = ThroughputCalculator.getMinimumTphValueByHour(throughputByProcess);

    assertEquals(expected, response);
  }
}
