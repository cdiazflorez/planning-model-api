package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NextSlaWaveProjectorTest {

  private static final Instant FIRST_INFLECTION_POINT = Instant.parse("2023-03-06T00:00:00Z");

  private static final Instant LAST_INFLECTION_POINT = Instant.parse("2023-03-06T03:55:00Z");

  private static final List<Instant> INFLECTION_POINTS = Stream.iterate(FIRST_INFLECTION_POINT, date -> date.plus(5, ChronoUnit.MINUTES))
      .limit((ChronoUnit.MINUTES.between(FIRST_INFLECTION_POINT, LAST_INFLECTION_POINT) / 5) + 1)
      .collect(Collectors.toList());

  private static final Map<ProcessPath, Map<Instant, Integer>> THROUGHPUT = Map.of(
      TOT_MONO, Map.of(
          Instant.parse("2023-03-06T00:00:00Z"), 60,
          Instant.parse("2023-03-06T01:00:00Z"), 60,
          Instant.parse("2023-03-06T02:00:00Z"), 60,
          Instant.parse("2023-03-06T03:00:00Z"), 60,
          Instant.parse("2023-03-06T04:00:00Z"), 60,
          Instant.parse("2023-03-06T05:00:00Z"), 60
      )
  );

  private static final Instant SLA_1 = Instant.parse("2023-03-06T03:00:00Z");

  private static final Instant SLA_2 = Instant.parse("2023-03-06T04:00:00Z");

  private static final Instant SLA_3 = Instant.parse("2023-03-06T05:00:00Z");

  private static final Map<ProcessPath, Map<Instant, Integer>> CURRENT_BACKLOG = Map.of(
      TOT_MONO, Map.of(SLA_1, 17, SLA_2, 0)
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

  private static final Map<ProcessPath, Integer> MIN_CYCLE_TIMES = Map.of(TOT_MONO, 60);

  private static Instant inflectionPoint(final int id) {
    return FIRST_INFLECTION_POINT.plus(id * 5L, ChronoUnit.MINUTES);
  }

  @Test
  void testNextWave() {
    // WHEN
    final var result = NextSlaWaveProjector.nextWave(
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
    assertEquals(expectedWaveDate, wave.getDate());

    final var units = wave.getConfiguration();
    assertEquals(1, units.size());
    assertEquals(5L, units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_1));
    assertEquals(140L, units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_2));
    assertFalse(units.get(TOT_MONO).getWavedUnitsByCpt().containsKey(SLA_3));
  }

  @Test
  @DisplayName("on next wave with two inflection points to project, then return empty")
  void testNextWaveWithFewInflectionPOints() {
    // WHEN
    final var result = NextSlaWaveProjector.nextWave(
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

    final var result = NextSlaWaveProjector.nextWave(
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
    assertEquals(expectedWaveDate, wave.getDate());

    final var units = wave.getConfiguration();
    assertEquals(1, units.size());
    assertEquals(5L, units.get(TOT_MONO).getWavedUnitsByCpt().get(SLA_2));
  }
}
