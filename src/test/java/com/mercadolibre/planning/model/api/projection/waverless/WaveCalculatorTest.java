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
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import com.mercadolibre.planning.model.api.projection.ProcessPathConfiguration;
import com.mercadolibre.planning.model.api.projection.UnitsByProcessPathAndProcess;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class WaveCalculatorTest {
  private static final Instant FIRST_INFLECTION_POINT = Instant.parse("2023-03-29T00:00:00Z");
  private static final String WH = "ARBA01";

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
      new ForecastedUnitsByProcessPath(TOT_MONO, DATES[3], SLA_2, 1360)
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
        new UnitsByProcessPathAndProcess(NON_TOT_MONO, WAVING, DATES[3], 800),
        new UnitsByProcessPathAndProcess(TOT_MULTI_BATCH, WAVING, DATES[3], 200),
        new UnitsByProcessPathAndProcess(TOT_MONO, WAVING, DATES[5], 2200),
        new UnitsByProcessPathAndProcess(NON_TOT_MONO, WAVING, DATES[5], 2200),
        new UnitsByProcessPathAndProcess(TOT_MULTI_BATCH, WAVING, DATES[5], 2200),

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
    // WHEN
    final var triggers = WavesCalculator.waves(
        FIRST_INFLECTION_POINT,
        CONFIGURATIONS,
        backlog,
        FORECAST,
        throughput,
        emptyMap(),
        WH
    );
    // THEN
    final var result = triggers.getWaves();

    assertEquals(4, result.size());
    // first wave
    final var firstWave = result.get(0);
    final var firstWaveExpectedDate = Instant.parse("2023-03-29T00:00:00Z");
    assertEquals(firstWaveExpectedDate, firstWave.getDate());
    assertEquals(TriggerName.SLA, firstWave.getReason());

    final var firstWaveTotMonoConf = firstWave.getConfiguration().get(NON_TOT_MONO);
    assertEquals(3000L, firstWaveTotMonoConf.getLowerBound());

    // fourth wave
    final var fourthWave = result.get(3);
    final var fourthWaveExpectedDate = Instant.parse("2023-03-29T03:25:00Z");
    assertEquals(fourthWaveExpectedDate, fourthWave.getDate());
    assertEquals(TriggerName.IDLENESS, fourthWave.getReason());

    final var fourthWaveTotMonoConf = fourthWave.getConfiguration().get(TOT_MULTI_BATCH);
    assertEquals(1200L, fourthWaveTotMonoConf.getLowerBound());
    assertEquals(1800L, fourthWaveTotMonoConf.getUpperBound());

    final var fourthWaveNonTotMonoConf = fourthWave.getConfiguration().get(NON_TOT_MONO);
    assertEquals(1200L, fourthWaveNonTotMonoConf.getLowerBound());
    assertEquals(1800L, fourthWaveNonTotMonoConf.getUpperBound());

    final var fourthWaveTotMultiBatchConf = fourthWave.getConfiguration().get(TOT_MULTI_BATCH);
    assertEquals(1200L, fourthWaveTotMultiBatchConf.getLowerBound());
    assertEquals(1800L, fourthWaveTotMultiBatchConf.getUpperBound());

    // projections
    assertEquals(72, triggers.getProjectedBacklogs().get(PICKING).size());

    assertEquals(1811, triggers.getProjectedBacklogs().get(PICKING).get(fourthWaveExpectedDate));
    final var nextInflectionPointAfterFirstWave = Instant.parse("2023-03-29T01:25:00Z");
    assertEquals(8521, triggers.getProjectedBacklogs().get(PICKING).get(nextInflectionPointAfterFirstWave));

  }
}
