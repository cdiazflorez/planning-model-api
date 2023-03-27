package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.FIRST_INFLECTION_POINT;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.INFLECTION_POINTS;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.MIN_CYCLE_TIMES;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_1;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_2;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_3;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.THROUGHPUT;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.inflectionPoint;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog.AvailableBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.SlaWaveCalculator.CurrentBacklog;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Value;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SlaWaveCalculatorTest {

  private static List<CurrentBacklog> projectedBacklog(final int sla1, final int sla2) {
    return List.of(
        new CurrentBacklog(TOT_MONO, SLA_1, sla1),
        new CurrentBacklog(TOT_MONO, SLA_2, sla2)
    );
  }

  static Stream<Arguments> parameters() {
    return Stream.of(
        argumentsForTestWithMultipleCascadingSlas(),
        argumentsForTestWithMultipleCascadingSlasWithInitialBacklog(),
        argumentsForTestWithMultipleCascadingSlasWithInitialBacklogAndForecast()
    );
  }

  private static Arguments argumentsForTestWithMultipleCascadingSlas() {
    return Arguments.of(
        emptyMap(),
        Map.of(
            TOT_MONO, List.of(
                new AvailableBacklog(SLA_1, SLA_1, 10D),
                new AvailableBacklog(SLA_1, SLA_2, 140D),
                new AvailableBacklog(SLA_1, SLA_3, 1D)
            )
        ),
        emptyMap(),
        Map.of(SLA_1, 10L, SLA_2, 140L)
    );
  }

  private static Arguments argumentsForTestWithMultipleCascadingSlasWithInitialBacklog() {
    return Arguments.of(
        Map.of(
            inflectionPoint(0), projectedBacklog(17, 30),
            inflectionPoint(1), projectedBacklog(15, 30),
            inflectionPoint(2), projectedBacklog(13, 30),
            inflectionPoint(3), projectedBacklog(11, 30),
            inflectionPoint(4), projectedBacklog(9, 30),
            inflectionPoint(5), projectedBacklog(7, 30),
            inflectionPoint(6), projectedBacklog(5, 30),
            inflectionPoint(7), projectedBacklog(2, 30)
        ),
        Map.of(
            TOT_MONO, List.of(
                new AvailableBacklog(SLA_1, SLA_1, 5D),
                new AvailableBacklog(SLA_1, SLA_2, 110D),
                new AvailableBacklog(SLA_1, SLA_3, 1D)
            )
        ),
        emptyMap(),
        Map.of(SLA_1, 5L, SLA_2, 110L)
    );
  }

  private static Arguments argumentsForTestWithMultipleCascadingSlasWithInitialBacklogAndForecast() {
    return Arguments.of(
        Map.of(
            inflectionPoint(0), projectedBacklog(17, 0),
            inflectionPoint(1), projectedBacklog(15, 0),
            inflectionPoint(2), projectedBacklog(13, 0),
            inflectionPoint(3), projectedBacklog(11, 0),
            inflectionPoint(4), projectedBacklog(9, 0),
            inflectionPoint(5), projectedBacklog(7, 0),
            inflectionPoint(6), projectedBacklog(5, 0),
            inflectionPoint(7), projectedBacklog(2, 0)
        ),
        Map.of(
            TOT_MONO, List.of(
                new AvailableBacklog(SLA_1, SLA_1, 5D),
                new AvailableBacklog(SLA_1, SLA_2, 110D),
                new AvailableBacklog(SLA_1, SLA_3, 1D)
            )
        ),
        Map.of(
            TOT_MONO, List.of(
                new AvailableBacklog(inflectionPoint(0), SLA_2, 60D)
            )
        ),
        Map.of(SLA_1, 5L, SLA_2, 140L)
    );
  }

  private static RequestTest generateRequestTest(final List<Wave> waves) {
    final Map<Instant, List<CurrentBacklog>> currentBacklog = emptyMap();

    final Map<ProcessPath, List<AvailableBacklog>> readyToWave = Map.of(
        TOT_MONO, List.of(new AvailableBacklog(SLA_1, SLA_1, 110D))
    );

    final Map<ProcessPath, List<AvailableBacklog>> forecast = emptyMap();

    final PendingBacklog pending = new PendingBacklog(readyToWave, forecast);

    return new RequestTest(
        INFLECTION_POINTS,
        currentBacklog,
        THROUGHPUT,
        pending,
        MIN_CYCLE_TIMES,
        waves
    );
  }

  @Test
  void testWithOnlyOnSla() {
    // GIVEN
    final RequestTest requestTest = generateRequestTest(emptyList());

    // WHEN
    var result = SlaWaveCalculator.projectNextWave(
        requestTest.getInflectionPoints(),
        requestTest.getProjectedBacklogs(),
        requestTest.getThroughput(),
        requestTest.getPendingBacklog(),
        requestTest.getMinCycleTimes(),
        emptyList()
    );

    // THEN
    assertTrue(result.isPresent());

    final var expectedWaveDate = Instant.parse("2023-03-06T00:10:00Z");

    final var wave = result.get();
    assertEquals(expectedWaveDate, wave.getDate());

    final var units = wave.getConfiguration();
    assertEquals(1, units.size());
    assertEquals(110L, units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_1));
  }

  @Test
  void testWithSlaAndWaves() {
    // GIVEN
    final RequestTest requestTest = generateRequestTest(emptyList());

    var result = SlaWaveCalculator.projectNextWave(
        requestTest.getInflectionPoints(),
        requestTest.getProjectedBacklogs(),
        requestTest.getThroughput(),
        requestTest.getPendingBacklog(),
        requestTest.getMinCycleTimes(),
        List.of(new Wave(
            FIRST_INFLECTION_POINT,
            TriggerName.SLA,
            Map.of(
                TOT_MONO,
                new Wave.WaveConfiguration(
                    110,
                    90000000,
                    Map.of(SLA_1, 10L)
                )
            )
        ))
    );

    // THEN
    assertTrue(result.isPresent());

    final var wave = result.get();

    final var units = wave.getConfiguration();
    assertEquals(1, units.size());
    assertEquals(100L, units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_1));
  }

  @ParameterizedTest
  @MethodSource("parameters")
  void testNextWave(
      final Map<Instant, List<CurrentBacklog>> currentBacklog,
      final Map<ProcessPath, List<AvailableBacklog>> readyToWave,
      final Map<ProcessPath, List<AvailableBacklog>> forecast,
      final Map<Instant, Long> expectedUnits
  ) {
    // GIVEN
    final PendingBacklog pending = new PendingBacklog(readyToWave, forecast);

    // WHEN
    var result = SlaWaveCalculator.projectNextWave(
        INFLECTION_POINTS,
        currentBacklog,
        THROUGHPUT,
        pending,
        MIN_CYCLE_TIMES,
        emptyList()
    );

    // THEN
    assertTrue(result.isPresent());

    final var expectedWaveDate = Instant.parse("2023-03-06T00:30:00Z");

    final var wave = result.get();
    assertEquals(expectedWaveDate, wave.getDate());

    final var units = wave.getConfiguration();
    assertEquals(1, units.size());
    assertEquals(expectedUnits.get(SLA_1), units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_1));
    assertEquals(expectedUnits.get(SLA_2), units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_2));
    assertFalse(units.get(TOT_MONO).getWavedUnitsByCpt().containsKey(SLA_3));
  }

  @Value
  private static class RequestTest {
    List<Instant> inflectionPoints;

    Map<Instant, List<SlaWaveCalculator.CurrentBacklog>> projectedBacklogs;

    Map<ProcessPath, Map<Instant, Integer>> throughput;

    PendingBacklog pendingBacklog;

    Map<ProcessPath, Integer> minCycleTimes;

    List<Wave> waves;
  }
}
