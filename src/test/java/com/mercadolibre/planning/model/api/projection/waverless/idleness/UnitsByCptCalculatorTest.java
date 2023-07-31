package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog.AvailableBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.PrecalculatedWave;
import com.mercadolibre.planning.model.api.projection.waverless.Wave;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UnitsByCptCalculatorTest {

  private static final Instant WAVE_EXECUTION_DATE = Instant.parse("2023-03-06T01:15:00Z");

  private static final Instant SLA_1 = Instant.parse("2023-03-06T03:00:00Z");

  private static final Instant SLA_2 = Instant.parse("2023-03-06T04:00:00Z");

  private static final Instant SLA_3 = Instant.parse("2023-03-06T05:00:00Z");

  private static final Instant SLA_4 = Instant.parse("2023-03-06T06:00:00Z");

  private static final Map<ProcessPath, Integer> UPPER_BOUNDS = Map.of(
      TOT_MONO, 1000,
      NON_TOT_MONO, 1700,
      TOT_MULTI_BATCH, 1500
  );

  private static final Map<ProcessPath, Integer> LOWER_BOUNDS = Map.of(
      TOT_MONO, 100,
      NON_TOT_MONO, 170,
      TOT_MULTI_BATCH, 150
  );

  private static final List<AvailableBacklog> AVAILABLE_BACKLOGS = List.of(
      new AvailableBacklog(SLA_1, SLA_1, 400D),
      new AvailableBacklog(SLA_1, SLA_2, 800D),
      new AvailableBacklog(SLA_1, SLA_3, 900D),
      new AvailableBacklog(SLA_1, SLA_4, 900D)

  );

  private static final Map<ProcessPath, List<AvailableBacklog>> READY_TO_WAVE = Map.of(
      TOT_MONO, AVAILABLE_BACKLOGS,
      NON_TOT_MONO, AVAILABLE_BACKLOGS,
      TOT_MULTI_BATCH, AVAILABLE_BACKLOGS
  );

  private static final PendingBacklog PENDING_BACKLOG = new PendingBacklog(READY_TO_WAVE, emptyMap());

  @Test
  void testWithoutPrecalculatedWaves() {
    // WHEN
    final var result = UnitsByCptCalculator.calculateBacklogToWave(
        WAVE_EXECUTION_DATE,
        emptyList(),
        PENDING_BACKLOG,
        UPPER_BOUNDS,
        LOWER_BOUNDS,
        emptyMap()
    );

    // THEN
    assertNotNull(result);

    final var totMono = result.get(TOT_MONO);
    assertNotNull(totMono);
    assertEquals(400L, totMono.get(SLA_1));
    assertEquals(600L, totMono.get(SLA_2));

    final var nonTotMono = result.get(NON_TOT_MONO);
    assertNotNull(nonTotMono);
    assertEquals(400L, nonTotMono.get(SLA_1));
    assertEquals(800L, nonTotMono.get(SLA_2));
    assertEquals(500L, nonTotMono.get(SLA_3));

    final var totMultiBatch = result.get(TOT_MULTI_BATCH);
    assertNotNull(totMultiBatch);
    assertEquals(400L, totMultiBatch.get(SLA_1));
    assertEquals(800L, totMultiBatch.get(SLA_2));
    assertEquals(300L, totMultiBatch.get(SLA_3));
  }

  @Test
  void testLessPrecalculatedWavesThanPreviousWaves() {
    // GIVEN
    final var previousWaves = List.of(
        new Wave(WAVE_EXECUTION_DATE, TriggerName.IDLENESS, emptyMap()),
        new Wave(WAVE_EXECUTION_DATE, TriggerName.IDLENESS, emptyMap())
    );

    final var precalculatedWave = List.of(new PrecalculatedWave(Map.of(SLA_1, 100L, SLA_2, 200L)));

    final var precalculatedWaves = Map.of(
        TOT_MONO, precalculatedWave,
        NON_TOT_MONO, precalculatedWave,
        TOT_MULTI_BATCH, precalculatedWave
    );

    // WHEN
    final var result = UnitsByCptCalculator.calculateBacklogToWave(
        WAVE_EXECUTION_DATE,
        previousWaves,
        PENDING_BACKLOG,
        UPPER_BOUNDS,
        LOWER_BOUNDS,
        precalculatedWaves
    );

    // THEN
    assertNotNull(result);

    final var totMono = result.get(TOT_MONO);
    assertNotNull(totMono);
    assertEquals(400L, totMono.get(SLA_1));
    assertEquals(600L, totMono.get(SLA_2));

    final var nonTotMono = result.get(NON_TOT_MONO);
    assertNotNull(nonTotMono);
    assertEquals(400L, nonTotMono.get(SLA_1));
    assertEquals(800L, nonTotMono.get(SLA_2));
    assertEquals(500L, nonTotMono.get(SLA_3));

    final var totMultiBatch = result.get(TOT_MULTI_BATCH);
    assertNotNull(totMultiBatch);
    assertEquals(400L, totMultiBatch.get(SLA_1));
    assertEquals(800L, totMultiBatch.get(SLA_2));
    assertEquals(300L, totMultiBatch.get(SLA_3));
  }

  @Test
  void testWithPrecalculatedWaveInBounds() {
    // GIVEN
    final var previousWaves = List.of(
        new Wave(WAVE_EXECUTION_DATE, TriggerName.SLA, emptyMap()),
        new Wave(WAVE_EXECUTION_DATE, TriggerName.IDLENESS, emptyMap())
    );

    final var precalculatedWaves = Map.of(
        TOT_MONO, List.of(
            new PrecalculatedWave(Map.of(SLA_1, 0L, SLA_2, 0L)),
            new PrecalculatedWave(Map.of(SLA_1, 200L, SLA_2, 300L))
        ),
        NON_TOT_MONO, List.of(
            new PrecalculatedWave(Map.of(SLA_1, 0L, SLA_2, 0L)),
            new PrecalculatedWave(Map.of(SLA_2, 350L, SLA_3, 500L))
        ),
        TOT_MULTI_BATCH, List.of(
            new PrecalculatedWave(Map.of(SLA_1, 0L, SLA_2, 0L)),
            new PrecalculatedWave(Map.of(SLA_1, 100L, SLA_2, 100L, SLA_3, 100L))
        )
    );

    // WHEN
    final var result = UnitsByCptCalculator.calculateBacklogToWave(
        WAVE_EXECUTION_DATE,
        previousWaves,
        PENDING_BACKLOG,
        UPPER_BOUNDS,
        LOWER_BOUNDS,
        precalculatedWaves
    );

    // THEN
    assertNotNull(result);

    final var totMono = result.get(TOT_MONO);
    assertNotNull(totMono);
    assertEquals(200L, totMono.get(SLA_1));
    assertEquals(300L, totMono.get(SLA_2));

    final var nonTotMono = result.get(NON_TOT_MONO);
    assertNotNull(nonTotMono);
    assertEquals(350L, nonTotMono.get(SLA_2));
    assertEquals(500L, nonTotMono.get(SLA_3));

    final var totMultiBatch = result.get(TOT_MULTI_BATCH);
    assertNotNull(totMultiBatch);
    assertEquals(100L, totMultiBatch.get(SLA_1));
    assertEquals(100L, totMultiBatch.get(SLA_2));
    assertEquals(100L, totMultiBatch.get(SLA_3));
  }

  @Test
  void testWithPrecalculatedWaveOutOfBounds() {
    // GIVEN
    final var previousWaves = List.of(
        new Wave(WAVE_EXECUTION_DATE, TriggerName.SLA, emptyMap()),
        new Wave(WAVE_EXECUTION_DATE, TriggerName.IDLENESS, emptyMap())
    );

    final var precalculatedWaves = Map.of(
        TOT_MONO, List.of(
            new PrecalculatedWave(Map.of(SLA_1, 0L, SLA_2, 0L)),
            new PrecalculatedWave(Map.of(SLA_1, 500L, SLA_2, 1000L))
        ),
        NON_TOT_MONO, List.of(
            new PrecalculatedWave(Map.of(SLA_1, 0L, SLA_2, 0L)),
            new PrecalculatedWave(Map.of(SLA_2, 50L, SLA_3, 75L))
        ),
        TOT_MULTI_BATCH, List.of(
            new PrecalculatedWave(Map.of(SLA_1, 0L, SLA_2, 0L)),
            new PrecalculatedWave(Map.of(SLA_1, 1000L, SLA_2, 1000L, SLA_3, 1000L))
        )
    );

    // WHEN
    final var result = UnitsByCptCalculator.calculateBacklogToWave(
        WAVE_EXECUTION_DATE,
        previousWaves,
        PENDING_BACKLOG,
        UPPER_BOUNDS,
        LOWER_BOUNDS,
        precalculatedWaves
    );

    // THEN
    assertNotNull(result);

    final var totMono = result.get(TOT_MONO);
    assertNotNull(totMono);

    // 1000 + 500 -> adjust -> 666 + 333 -> cap -> 666 + 333 = 999
    assertEquals(333L, totMono.get(SLA_1));
    assertEquals(666L, totMono.get(SLA_2));

    final var nonTotMono = result.get(NON_TOT_MONO);
    assertNotNull(nonTotMono);

    // 50 + 75 -> adjust -> 68 + 102 -> cap -> 68 + 102 = 170
    assertEquals(68L, nonTotMono.get(SLA_2));
    assertEquals(102L, nonTotMono.get(SLA_3));

    final var totMultiBatch = result.get(TOT_MULTI_BATCH);
    assertNotNull(totMultiBatch);

    // 1000 + 1000 + 1000 -> adjust -> 500 + 500 + 500 -> cap -> 400 + 500 + 500 = 1500
    assertEquals(400L, totMultiBatch.get(SLA_1));
    assertEquals(600L, totMultiBatch.get(SLA_2));
    assertEquals(500L, totMultiBatch.get(SLA_3));
  }

  @Test
  void testWithPrecalculatedWaveInBoundsButWithoutAvailableBacklog() {
    // GIVEN
    final var previousWaves = List.of(
        new Wave(WAVE_EXECUTION_DATE, TriggerName.IDLENESS, emptyMap())
    );

    final var precalculatedWaves = Map.of(
        TOT_MONO, List.of(
            new PrecalculatedWave(Map.of(SLA_1, 0L, SLA_2, 0L)),
            new PrecalculatedWave(Map.of(SLA_1, 500L, SLA_2, 300L))
        ),
        NON_TOT_MONO, List.of(
            new PrecalculatedWave(Map.of(SLA_1, 0L, SLA_2, 0L)),
            new PrecalculatedWave(Map.of(SLA_2, 950L, SLA_3, 100L))
        ),
        TOT_MULTI_BATCH, List.of(
            new PrecalculatedWave(Map.of(SLA_1, 0L, SLA_2, 0L)),
            new PrecalculatedWave(Map.of(SLA_1, 100L, SLA_2, 100L, SLA_3, 1000L))
        )
    );

    // WHEN
    final var result = UnitsByCptCalculator.calculateBacklogToWave(
        WAVE_EXECUTION_DATE,
        previousWaves,
        PENDING_BACKLOG,
        UPPER_BOUNDS,
        LOWER_BOUNDS,
        precalculatedWaves
    );

    // THEN
    assertNotNull(result);

    final var totMono = result.get(TOT_MONO);
    assertNotNull(totMono);
    assertEquals(400L, totMono.get(SLA_1));
    assertEquals(400L, totMono.get(SLA_2));

    final var nonTotMono = result.get(NON_TOT_MONO);
    assertNotNull(nonTotMono);
    assertEquals(800L, nonTotMono.get(SLA_2));
    assertEquals(100L, nonTotMono.get(SLA_3));

    final var totMultiBatch = result.get(TOT_MULTI_BATCH);
    assertNotNull(totMultiBatch);
    assertEquals(200L, totMultiBatch.get(SLA_1));
    assertEquals(100L, totMultiBatch.get(SLA_2));
    assertEquals(900L, totMultiBatch.get(SLA_3));
  }

}
