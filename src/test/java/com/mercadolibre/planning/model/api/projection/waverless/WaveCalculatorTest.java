package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_2;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.inflectionPoint;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;

import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import com.mercadolibre.planning.model.api.projection.ProcessPathConfiguration;
import com.mercadolibre.planning.model.api.projection.UnitsByProcessPathAndProcess;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class WaveCalculatorTest {
  private static final Instant FIRST_INFLECTION_POINT = Instant.parse("2023-03-29T00:00:00Z");

  private static final Instant[] DATES = {
      Instant.parse("2023-03-29T00:00:00Z"),
      Instant.parse("2023-03-29T01:00:00Z"),
      Instant.parse("2023-03-29T02:00:00Z"),
      Instant.parse("2023-03-29T03:00:00Z"),
      Instant.parse("2023-03-29T04:00:00Z"),
      Instant.parse("2023-03-29T05:00:00Z"),
      Instant.parse("2023-03-29T06:00:00Z"),
  };

  private static final List<ForecastedUnitsByProcessPath> FORECAST = List.of(
      new ForecastedUnitsByProcessPath(TOT_MONO, inflectionPoint(0), SLA_2, 60)
  );

  private static final List<ProcessPathConfiguration> CONFIGURATIONS = List.of(
      new ProcessPathConfiguration(TOT_MONO, 120, 100, 60),
      new ProcessPathConfiguration(NON_TOT_MONO, 120, 100, 60),
      new ProcessPathConfiguration(TOT_MULTI_BATCH, 120, 100, 60)
  );

  private MockedStatic<ExecutionMetrics.DataDogMetricsWrapper> wrapper;

  private static Map<Instant, Integer> throughput(int v1, int v2, int v3, int v4, int v5, int v6) {
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

  @BeforeEach
  public void setUp() {
    wrapper = mockStatic(ExecutionMetrics.DataDogMetricsWrapper.class);
  }

  @AfterEach
  public void tearDown() {
    wrapper.close();
  }

  @Test
  void testCalculateBothTypeOfWaves() {
    // GIVEN
    final var backlog = List.of(
        new UnitsByProcessPathAndProcess(TOT_MONO, WAVING, DATES[6], 6500),
        new UnitsByProcessPathAndProcess(NON_TOT_MONO, WAVING, DATES[6], 6500),
        new UnitsByProcessPathAndProcess(TOT_MULTI_BATCH, WAVING, DATES[6], 6500),

        new UnitsByProcessPathAndProcess(TOT_MONO, PICKING, DATES[0], 2500),
        new UnitsByProcessPathAndProcess(NON_TOT_MONO, PICKING, DATES[0], 1500),
        new UnitsByProcessPathAndProcess(TOT_MULTI_BATCH, PICKING, DATES[0], 2000),

        new UnitsByProcessPathAndProcess(TOT_MONO, PACKING, DATES[0], 3000),
        new UnitsByProcessPathAndProcess(NON_TOT_MONO, PACKING, DATES[0], 800),

        new UnitsByProcessPathAndProcess(TOT_MULTI_BATCH, BATCH_SORTER, DATES[0], 1780),
        new UnitsByProcessPathAndProcess(TOT_MULTI_BATCH, WALL_IN, DATES[0], 490),
        new UnitsByProcessPathAndProcess(TOT_MULTI_BATCH, PACKING_WALL, DATES[0], 800)
    );

    final var throughput = Map.of(
        GLOBAL, Map.of(
            PICKING, throughput(3600, 3600, 3600, 3600, 3600, 3600),
            PACKING, throughput(2800, 2800, 2800, 2800, 2800, 2800),
            BATCH_SORTER, throughput(1560, 1560, 1560, 1560, 1560, 1560),
            WALL_IN, throughput(1560, 1560, 1560, 1560, 1560, 1560),
            PACKING_WALL, throughput(1560, 1560, 1560, 1560, 1560, 1560)
        ),
        TOT_MONO, Map.of(
            PICKING, throughput(1200, 1200, 1200, 1200, 1200, 1200)
        ),
        NON_TOT_MONO, Map.of(
            PICKING, throughput(1200, 1200, 1200, 1200, 1200, 1200)
        ),
        TOT_MULTI_BATCH, Map.of(
            PICKING, throughput(1200, 1200, 1200, 1200, 1200, 1200)
        )
    );

    final var upperLimits = Map.of(
        PICKING, throughput(9000, 9000, 9000, 9000, 9000, 9000),
        PACKING, throughput(9000, 9000, 9000, 9000, 9000, 9000),
        BATCH_SORTER, throughput(6000, 6000, 6000, 6000, 6000, 6000),
        WALL_IN, throughput(6000, 6000, 6000, 6000, 6000, 6000),
        PACKING_WALL, throughput(6000, 6000, 6000, 6000, 6000, 6000)
    );

    final var lowerLimits = Map.of(
        PICKING, throughput(1100, 1100, 1100, 1100, 1100, 1100)
    );

    // WHEN
    final var result = WavesCalculator.waves(
        FIRST_INFLECTION_POINT,
        CONFIGURATIONS,
        backlog,
        FORECAST,
        throughput,
        new BacklogLimits(lowerLimits, upperLimits),
        emptyMap()
    );

    // THEN
    assertEquals(3, result.size());

    // first wave
    final var firstWave = result.get(0);
    final var firstWaveExpectedDate = Instant.parse("2023-03-29T01:20:00Z");
    assertEquals(firstWaveExpectedDate, firstWave.getDate());
    assertEquals(TriggerName.IDLENESS, firstWave.getReason());

    final var firstWaveTotMonoConf = firstWave.getConfiguration().get(TOT_MONO);
    assertEquals(600L, firstWaveTotMonoConf.getLowerBound());
    assertEquals(2699L, firstWaveTotMonoConf.getUpperBound());

    final var firstWaveNonTotMonoConf = firstWave.getConfiguration().get(NON_TOT_MONO);
    assertEquals(600L, firstWaveNonTotMonoConf.getLowerBound());
    assertEquals(2699L, firstWaveNonTotMonoConf.getUpperBound());

    final var firstWaveTotMultiBatchConf = firstWave.getConfiguration().get(TOT_MULTI_BATCH);
    assertEquals(600L, firstWaveTotMultiBatchConf.getLowerBound());
    assertEquals(2699L, firstWaveTotMultiBatchConf.getUpperBound());

    // second wave
    final var secondWave = result.get(1);
    final var secondWaveExpectedDate = Instant.parse("2023-03-29T01:45:00Z");
    assertEquals(secondWaveExpectedDate, secondWave.getDate());
    assertEquals(TriggerName.SLA, secondWave.getReason());

    // 2500 - 2100 = 400 + 6500 = 6900 -->
    final var secondWaveTotMonoConf = secondWave.getConfiguration().get(TOT_MONO);
    assertNull(secondWaveTotMonoConf);

    final var secondWaveNonTotMonoConf = secondWave.getConfiguration().get(NON_TOT_MONO);

    // 6500 - 2699 = 3801
    // tph(2023-03-29T01:45:00Z, 2023-03-29T05:00:00Z) = 1200 * 3.25 = 3900
    assertEquals(3801L, secondWaveNonTotMonoConf.getLowerBound());
    assertEquals(Set.of(DATES[6]), secondWaveNonTotMonoConf.getWavedUnitsByCpt().keySet());

    final var secondWaveTotMultiBatchConf = secondWave.getConfiguration().get(TOT_MULTI_BATCH);

    // 6500 - 2699 = 3801
    // tph(2023-03-29T01:45:00Z, 2023-03-29T05:00:00Z) = 1200 * 3.25 = 3900
    assertEquals(3801L, secondWaveTotMultiBatchConf.getLowerBound());
    assertEquals(Set.of(DATES[6]), secondWaveTotMultiBatchConf.getWavedUnitsByCpt().keySet());

    // third wave
    final var thirdWave = result.get(2);
    final var thirdWaveExpectedDate = Instant.parse("2023-03-29T05:40:00Z");
    assertEquals(thirdWaveExpectedDate, thirdWave.getDate());
    assertEquals(TriggerName.IDLENESS, thirdWave.getReason());

    final var thirdWaveTotMonoConf = thirdWave.getConfiguration().get(TOT_MONO);
    assertEquals(566L, thirdWaveTotMonoConf.getLowerBound());
    assertEquals(819L, thirdWaveTotMonoConf.getUpperBound());

    final var thirdWaveNonTotMonoConf = thirdWave.getConfiguration().get(NON_TOT_MONO);
    assertEquals(566L, thirdWaveNonTotMonoConf.getLowerBound());
    assertEquals(566L, thirdWaveNonTotMonoConf.getUpperBound());

    final var thirdWaveTotMultiBatchConf = thirdWave.getConfiguration().get(TOT_MULTI_BATCH);
    assertEquals(566L, thirdWaveTotMultiBatchConf.getLowerBound());
    assertEquals(566L, thirdWaveTotMultiBatchConf.getUpperBound());
  }
}
