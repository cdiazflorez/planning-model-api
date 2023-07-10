package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.FIRST_INFLECTION_POINT;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.INFLECTION_POINTS;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.MIN_CYCLE_TIMES;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.PICKING_THROUGHPUT;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_1;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_2;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_3;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.TPH;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.inflectionPoint;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog.AvailableBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.SlaWaveCalculator.CurrentBacklog;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

class SlaWaveCalculatorTest {

  private static final Instant FORECAST_DATE_IN = Instant.parse("2023-03-06T02:00:00Z");

  private MockedStatic<ExecutionMetrics.DataDogMetricsWrapper> wrapper;

  private static List<CurrentBacklog> projectedBacklog(final long sla1, final long sla2) {
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
            inflectionPoint(0), projectedBacklog(17L, 30L),
            inflectionPoint(1), projectedBacklog(15L, 30L),
            inflectionPoint(2), projectedBacklog(13L, 30L),
            inflectionPoint(3), projectedBacklog(11L, 30L),
            inflectionPoint(4), projectedBacklog(9L, 30L),
            inflectionPoint(5), projectedBacklog(7L, 30L),
            inflectionPoint(6), projectedBacklog(5L, 30L),
            inflectionPoint(7), projectedBacklog(2L, 30L)
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
            inflectionPoint(0), projectedBacklog(17L, 0L),
            inflectionPoint(1), projectedBacklog(15L, 0L),
            inflectionPoint(2), projectedBacklog(13L, 0L),
            inflectionPoint(3), projectedBacklog(11L, 0L),
            inflectionPoint(4), projectedBacklog(9L, 0L),
            inflectionPoint(5), projectedBacklog(7L, 0L),
            inflectionPoint(6), projectedBacklog(5L, 0L),
            inflectionPoint(7), projectedBacklog(2L, 0L)
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
        PICKING_THROUGHPUT,
        pending,
        MIN_CYCLE_TIMES,
        waves
    );
  }

  private static Stream<Arguments> forecastArguments() {
    return Stream.of(
        Arguments.of(
            Map.of(
                TOT_MONO, List.of(new AvailableBacklog(FORECAST_DATE_IN, SLA_2, 1200D))
            )
        ),
        Arguments.of(
            Map.of(
                TOT_MULTI_BATCH, List.of(new AvailableBacklog(FORECAST_DATE_IN, SLA_2, 1200D))
            )
        )
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
    assertEquals(expectedWaveDate, wave.getExecutionDate());

    final var units = wave.getWave().get().getConfiguration();
    assertEquals(1, units.size());
    assertEquals(110L, units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_1));
  }

  @BeforeEach
  public void setUp() {
    wrapper = mockStatic(ExecutionMetrics.DataDogMetricsWrapper.class);
  }

  @AfterEach
  public void tearDown() {
    wrapper.close();
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

    final var units = wave.getWave().get().getConfiguration();
    assertEquals(1, units.size());
    assertEquals(100L, units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_1));
  }

  @ParameterizedTest
  @MethodSource("forecastArguments")
  @DisplayName("forecasted backlog can not be waved before its date_in")
  void testWithoutCurrentBacklogAndWithForecastedBacklog(
      final Map<ProcessPath, List<AvailableBacklog>> forecast
  ) {
    // GIVEN
    final var readyToWave = Map.of(
        TOT_MONO, List.of(
            new AvailableBacklog(SLA_1, SLA_3, 10D)
        )
    );

    final PendingBacklog pending = new PendingBacklog(readyToWave, forecast);

    final var throughput = Map.of(
        TOT_MONO, TPH,
        TOT_MULTI_BATCH, TPH
    );

    // WHEN
    var result = SlaWaveCalculator.projectNextWave(
        INFLECTION_POINTS,
        emptyMap(),
        throughput,
        pending,
        MIN_CYCLE_TIMES,
        emptyList()
    );

    // THEN
    assertTrue(result.isPresent());

    final var wave = result.get();
    assertTrue(wave.getExecutionDate().isAfter(FORECAST_DATE_IN));
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
        PICKING_THROUGHPUT,
        pending,
        MIN_CYCLE_TIMES,
        emptyList()
    );

    // THEN
    assertTrue(result.isPresent());

    final var expectedWaveDate = Instant.parse("2023-03-06T00:30:00Z");

    final var wave = result.get();
    assertEquals(expectedWaveDate, wave.getExecutionDate());

    final var units = wave.getWave().get().getConfiguration();
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
