package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.FIRST_INFLECTION_POINT;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.INFLECTION_POINTS;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.LAST_INFLECTION_POINT;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.MIN_CYCLE_TIMES;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_1;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_2;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_3;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.THROUGHPUT;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.inflectionPoint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class NextSlaWaveProjectorTest {

  private static final Map<ProcessPath, Map<Instant, Long>> CURRENT_BACKLOG = Map.of(
      TOT_MONO, Map.of(SLA_1, 17L, SLA_2, 0L)
  );

  private static final Map<ProcessPath, List<PendingBacklog.AvailableBacklog>> READY_TO_WAVE = Map.of(
      TOT_MONO, List.of(
          new PendingBacklog.AvailableBacklog(SLA_1, SLA_1, 5D),
          new PendingBacklog.AvailableBacklog(SLA_1, SLA_2, 110D),
          new PendingBacklog.AvailableBacklog(SLA_1, SLA_3, 1D)
      )
  );

  private static final Map<ProcessPath, List<PendingBacklog.AvailableBacklog>> FORECAST = Map.of(
      TOT_MONO, List.of(
          new PendingBacklog.AvailableBacklog(inflectionPoint(0), SLA_2, 60D)
      )
  );

  private MockedStatic<ExecutionMetrics.DataDogMetricsWrapper> wrapper;

  @BeforeEach
  public void setUp() {
    wrapper = mockStatic(ExecutionMetrics.DataDogMetricsWrapper.class);
  }

  @AfterEach
  public void tearDown() {
    wrapper.close();
  }

  @Test
  void testNextWave() {
    // WHEN
    final var result = NextSlaWaveProjector.calculateNextWave(
        INFLECTION_POINTS,
        Collections.emptyList(),
        new PendingBacklog(READY_TO_WAVE, FORECAST),
        CURRENT_BACKLOG,
        THROUGHPUT,
        MIN_CYCLE_TIMES
    );

    // THEN
    assertTrue(result.isPresent());

    final var expectedWaveDate = Instant.parse("2023-03-06T00:30:00Z");

    final var wave = result.get();
    assertEquals(expectedWaveDate, wave.getExecutionDate());

    final var units = wave.getWave().get().getConfiguration();
    assertEquals(1, units.size());
    assertEquals(5L, units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_1));
    assertEquals(140L, units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_2));
    assertFalse(units.get(TOT_MONO).getWavedUnitsByCpt().containsKey(SLA_3));
  }

  @Test
  @DisplayName("on next wave with two inflection points to project, then return empty")
  void testNextWaveWithFewInflectionPOints() {
    // WHEN
    final var result = NextSlaWaveProjector.calculateNextWave(
        List.of(FIRST_INFLECTION_POINT, LAST_INFLECTION_POINT),
        Collections.emptyList(),
        new PendingBacklog(READY_TO_WAVE, FORECAST),
        CURRENT_BACKLOG,
        THROUGHPUT,
        MIN_CYCLE_TIMES
    );

    // THEN
    assertTrue(result.isEmpty());
  }

  @Test
  @DisplayName("on next wave with an initial wave, then the next wave must be after the first")
  void testNextWaveWithAnInitialWave() {
    // WHEN
    final var firstWave = new Wave(
        Instant.parse("2023-03-06T00:30:00Z"),
        TriggerName.SLA,
        Map.of(
            TOT_MONO, new Wave.WaveConfiguration(
                145, 145, Map.of(SLA_1, 5L, SLA_2, 140L)
            )
        )
    );

    final var result = NextSlaWaveProjector.calculateNextWave(
        INFLECTION_POINTS,
        List.of(firstWave),
        new PendingBacklog(READY_TO_WAVE, FORECAST),
        CURRENT_BACKLOG,
        THROUGHPUT,
        MIN_CYCLE_TIMES
    );

    // THEN
    assertTrue(result.isPresent());
    final var expectedWaveDate = Instant.parse("2023-03-06T00:35:00Z");

    final var wave = result.get();
    assertEquals(expectedWaveDate, wave.getExecutionDate());

    final var units = wave.getWave().get().getConfiguration();
    assertEquals(1, units.size());
    assertEquals(5L, units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_2));
  }
}
