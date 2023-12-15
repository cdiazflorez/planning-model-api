package com.mercadolibre.planning.model.api.projection.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class InstantThroughputTest {

  private static final Instant HOUR_1 = Instant.parse("2023-04-04T10:00:00Z");
  private static final Instant HOUR_2 = Instant.parse("2023-04-04T11:00:00Z");
  private static final Instant HOUR_3 = Instant.parse("2023-04-04T12:00:00Z");
  private static final Map<Instant, Integer> THROUGHPUTS = Map.of(
      HOUR_1, 1000,
      HOUR_2, 2000,
      HOUR_3, 500
  );

  public static Stream<Arguments> params() {
    return Stream.of(
        Arguments.of(
            HOUR_1,
            HOUR_1.plus(5, ChronoUnit.MINUTES),
            1000
        ),
        Arguments.of(
            HOUR_1.plus(5, ChronoUnit.MINUTES),
            HOUR_1.plus(10, ChronoUnit.MINUTES),
            0
        ),
        Arguments.of(
            HOUR_2,
            HOUR_1.plus(10, ChronoUnit.MINUTES),
            2000
        ),
        Arguments.of(
            HOUR_2.plus(5, ChronoUnit.MINUTES),
            HOUR_2.plus(10, ChronoUnit.MINUTES),
            0
        )
    );
  }

  @ParameterizedTest
  @MethodSource("params")
  @DisplayName("test ok")
  void availableBetween(
      final Instant from,
      final Instant to,
      final int expectedThroughput
  ) {
    final InstantThroughput instantThroughput = new InstantThroughput(THROUGHPUTS);
    final int throughput = instantThroughput.availableBetween(from, to);
    assertEquals(expectedThroughput, throughput);
  }

}
