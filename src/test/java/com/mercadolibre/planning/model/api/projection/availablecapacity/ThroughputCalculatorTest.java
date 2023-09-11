package com.mercadolibre.planning.model.api.projection.availablecapacity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


class ThroughputCalculatorTest {

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
}
