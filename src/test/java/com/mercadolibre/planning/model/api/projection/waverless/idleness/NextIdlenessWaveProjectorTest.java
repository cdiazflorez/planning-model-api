package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.util.DateUtils.generateInflectionPoints;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import com.mercadolibre.planning.model.api.projection.waverless.BacklogLimits;
import com.mercadolibre.planning.model.api.projection.waverless.ExecutionMetrics;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog.AvailableBacklog;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class NextIdlenessWaveProjectorTest {

  private static final Instant FIRST_INFLECTION_POINT = Instant.parse("2023-03-29T00:00:00Z");

  private static final Instant LAST_INFLECTION_POINT = Instant.parse("2023-03-29T05:55:00Z");

  private static final List<Instant> INFLECTION_POINTS = generateInflectionPoints(FIRST_INFLECTION_POINT, LAST_INFLECTION_POINT, 5);

  private static final Instant[] DATES = {
      Instant.parse("2023-03-29T00:00:00Z"),
      Instant.parse("2023-03-29T01:00:00Z"),
      Instant.parse("2023-03-29T02:00:00Z"),
      Instant.parse("2023-03-29T03:00:00Z"),
      Instant.parse("2023-03-29T04:00:00Z"),
      Instant.parse("2023-03-29T05:00:00Z"),
      Instant.parse("2023-03-29T06:00:00Z"),
  };

  private static final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> BACKLOGS = Map.of(
      PICKING, Map.of(
          TOT_MONO, Map.of(DATES[0], 2500L),
          NON_TOT_MONO, Map.of(DATES[0], 1500L),
          TOT_MULTI_BATCH, Map.of(DATES[0], 2000L)
      ),
      PACKING, Map.of(
          TOT_MONO, Map.of(DATES[0], 3000L),
          NON_TOT_MONO, Map.of(DATES[0], 800L)
      ),
      BATCH_SORTER, Map.of(
          NON_TOT_MONO, Map.of(DATES[0], 1780L)
      ),
      WALL_IN, Map.of(
          NON_TOT_MONO, Map.of(DATES[0], 490L)
      ),
      PACKING_WALL, Map.of(
          NON_TOT_MONO, Map.of(DATES[0], 800L)
      )
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
  void testWaveProjection() {
    // GIVEN
    final var throughput = Map.of(
        GLOBAL, Map.of(
            PICKING, throughput(3600, 3600, 3600, 3600, 3600, 3600),
            PACKING, throughput(1800, 1800, 1800, 1800, 1800, 1800),
            BATCH_SORTER, throughput(1560, 1560, 1560, 1560, 1560, 1560),
            WALL_IN, throughput(480, 480, 480, 480, 480, 480),
            PACKING_WALL, throughput(960, 960, 960, 960, 960, 960)
        ),
        TOT_MONO, Map.of(
            PICKING, throughput(480, 480, 480, 480, 480, 480)
        ),
        NON_TOT_MONO, Map.of(
            PICKING, throughput(480, 480, 480, 480, 480, 480)
        ),
        TOT_MULTI_BATCH, Map.of(
            PICKING, throughput(480, 480, 480, 480, 480, 480)
        )
    );

    final var readyToWave = Map.of(
        TOT_MONO, List.of(new AvailableBacklog(FIRST_INFLECTION_POINT, FIRST_INFLECTION_POINT, 1000D)),
        NON_TOT_MONO, List.of(new AvailableBacklog(FIRST_INFLECTION_POINT, FIRST_INFLECTION_POINT, 1000D)),
        TOT_MULTI_BATCH, List.of(new AvailableBacklog(FIRST_INFLECTION_POINT, FIRST_INFLECTION_POINT, 1000D))
    );

    final var pendingBacklog = new PendingBacklog(readyToWave, emptyMap());

    final var upperLimits = Map.of(
        PICKING, throughput(3000, 3000, 3000, 3000, 3000, 3000),
        PACKING, throughput(6000, 6000, 6000, 6000, 6000, 6000),
        BATCH_SORTER, throughput(6000, 6000, 6000, 6000, 6000, 6000),
        WALL_IN, throughput(6000, 6000, 6000, 6000, 6000, 6000),
        PACKING_WALL, throughput(6000, 6000, 6000, 6000, 6000, 6000)
    );

    final var lowerLimits = Map.of(
        PICKING, throughput(1100, 1100, 1100, 1100, 1100, 1100)
    );

    // WHEN
    final var result = NextIdlenessWaveProjector.calculateNextWave(
        INFLECTION_POINTS,
        pendingBacklog,
        BACKLOGS,
        throughput,
        new BacklogLimits(lowerLimits, upperLimits),
        emptyMap(),
        emptyList()
    );

    // THEN
    assertNotNull(result);
    assertTrue(result.isPresent());

    final var wave = result.get();

    final var expectedWaveDate = Instant.parse("2023-03-29T01:20:00Z");
    assertEquals(expectedWaveDate, wave.getExecutionDate());
    assertEquals(TriggerName.IDLENESS, wave.getWave().get().getReason());

    final var confs = wave.getWave().get().getConfiguration();
    assertEquals(699, confs.get(TOT_MONO).getUpperBound());
    assertEquals(240, confs.get(TOT_MONO).getLowerBound());

    assertEquals(699, confs.get(NON_TOT_MONO).getUpperBound());
    assertEquals(240, confs.get(NON_TOT_MONO).getLowerBound());

    assertEquals(699, confs.get(TOT_MULTI_BATCH).getUpperBound());
    assertEquals(240, confs.get(TOT_MULTI_BATCH).getLowerBound());
  }

  @Test
  void testWaveProjectionWithoutCapacityInPackingWall() {
    // GIVEN
    final var throughput = Map.of(
        GLOBAL, Map.of(
            PICKING, throughput(3600, 3600, 3600, 3600, 3600, 3600),
            PACKING, throughput(1800, 1800, 1800, 1800, 1800, 1800),
            BATCH_SORTER, throughput(1560, 1560, 1560, 1560, 1560, 1560),
            WALL_IN, throughput(480, 480, 480, 480, 480, 480),
            PACKING_WALL, throughput(960, 960, 960, 960, 960, 960)
        ),
        TOT_MONO, Map.of(
            PICKING, throughput(480, 480, 480, 480, 480, 480)
        ),
        NON_TOT_MONO, Map.of(
            PICKING, throughput(480, 480, 480, 480, 480, 480)
        ),
        TOT_MULTI_BATCH, Map.of(
            PICKING, throughput(480, 480, 480, 480, 480, 480)
        )
    );

    final var readyToWave = Map.of(
        TOT_MONO, List.of(new AvailableBacklog(FIRST_INFLECTION_POINT, FIRST_INFLECTION_POINT, 10000D)),
        NON_TOT_MONO, List.of(new AvailableBacklog(FIRST_INFLECTION_POINT, FIRST_INFLECTION_POINT, 10000D)),
        TOT_MULTI_BATCH, List.of(new AvailableBacklog(FIRST_INFLECTION_POINT, FIRST_INFLECTION_POINT, 10000D))
    );

    final var pendingBacklog = new PendingBacklog(readyToWave, emptyMap());

    final var upperLimits = Map.of(
        PICKING, throughput(3000, 3000, 3000, 3000, 3000, 3000),
        PACKING, throughput(6000, 6000, 6000, 6000, 6000, 6000),
        BATCH_SORTER, throughput(6000, 6000, 6000, 6000, 6000, 6000),
        WALL_IN, throughput(6000, 6000, 6000, 6000, 6000, 6000),
        PACKING_WALL, throughput(10, 10, 10, 10, 10, 10)
    );

    final var lowerLimits = Map.of(
        PICKING, throughput(1100, 1100, 1100, 1100, 1100, 1100)
    );

    // WHEN
    final var result = NextIdlenessWaveProjector.calculateNextWave(
        INFLECTION_POINTS,
        pendingBacklog,
        BACKLOGS,
        throughput,
        new BacklogLimits(lowerLimits, upperLimits),
        emptyMap(),
        emptyList()
    );

    // THEN
    assertNotNull(result);
    assertTrue(result.isPresent());

    final var wave = result.get();

    final var expectedWaveDate = Instant.parse("2023-03-29T01:20:00Z");
    assertEquals(expectedWaveDate, wave.getExecutionDate());
    assertEquals(TriggerName.IDLENESS, wave.getWave().get().getReason());

    final var confs = wave.getWave().get().getConfiguration();
    assertEquals(928, confs.get(TOT_MONO).getUpperBound());
    assertEquals(240, confs.get(TOT_MONO).getLowerBound());

    assertEquals(928, confs.get(NON_TOT_MONO).getUpperBound());
    assertEquals(240, confs.get(NON_TOT_MONO).getLowerBound());

    assertEquals(240, confs.get(TOT_MULTI_BATCH).getUpperBound());
    assertEquals(240, confs.get(TOT_MULTI_BATCH).getLowerBound());
  }

  @Test
  void testWaveProjectionWhenThereIsCurrentlyIdleness() {
    // GIVEN
    final var throughput = Map.of(
        GLOBAL, Map.of(
            PICKING, throughput(3600, 3600, 3600, 3600, 3600, 3600),
            PACKING, throughput(1800, 1800, 1800, 1800, 1800, 1800),
            BATCH_SORTER, throughput(1560, 1560, 1560, 1560, 1560, 1560),
            WALL_IN, throughput(480, 480, 480, 480, 480, 480),
            PACKING_WALL, throughput(960, 960, 960, 960, 960, 960)
        ),
        TOT_MONO, Map.of(
            PICKING, throughput(480, 480, 480, 480, 480, 480)
        ),
        NON_TOT_MONO, Map.of(
            PICKING, throughput(480, 480, 480, 480, 480, 480)
        ),
        TOT_MULTI_BATCH, Map.of(
            PICKING, throughput(480, 480, 480, 480, 480, 480)
        )
    );

    final var readyToWave = Map.of(
        TOT_MONO, List.of(new AvailableBacklog(FIRST_INFLECTION_POINT, FIRST_INFLECTION_POINT, 10000D)),
        NON_TOT_MONO, List.of(new AvailableBacklog(FIRST_INFLECTION_POINT, FIRST_INFLECTION_POINT, 10000D)),
        TOT_MULTI_BATCH, List.of(new AvailableBacklog(FIRST_INFLECTION_POINT, FIRST_INFLECTION_POINT, 10000D))
    );

    final var pendingBacklog = new PendingBacklog(readyToWave, emptyMap());

    final var upperLimits = Map.of(
        PICKING, throughput(13000, 13000, 13000, 13000, 13000, 13000),
        PACKING, throughput(6000, 6000, 6000, 6000, 6000, 6000),
        BATCH_SORTER, throughput(6000, 6000, 6000, 6000, 6000, 6000),
        WALL_IN, throughput(6000, 6000, 6000, 6000, 6000, 6000),
        PACKING_WALL, throughput(6000, 6000, 6000, 6000, 6000, 6000)
    );

    final var lowerLimits = Map.of(
        PICKING, throughput(10000, 10000, 10000, 10000, 10000, 10000)
    );

    // WHEN
    final var result = NextIdlenessWaveProjector.calculateNextWave(
        INFLECTION_POINTS,
        pendingBacklog,
        BACKLOGS,
        throughput,
        new BacklogLimits(lowerLimits, upperLimits),
        emptyMap(),
        emptyList()
    );

    // THEN
    assertNotNull(result);
    assertTrue(result.isPresent());

    final var wave = result.get();

    final var expectedWaveDate = Instant.parse("2023-03-29T00:00:00Z");
    assertEquals(expectedWaveDate, wave.getExecutionDate());
    assertEquals(TriggerName.IDLENESS, wave.getWave().get().getReason());

    final var confs = wave.getWave().get().getConfiguration();
    assertEquals(2433, confs.get(TOT_MONO).getUpperBound());
    assertEquals(1573, confs.get(TOT_MONO).getLowerBound());

    assertEquals(2433, confs.get(NON_TOT_MONO).getUpperBound());
    assertEquals(1573, confs.get(NON_TOT_MONO).getLowerBound());

    assertEquals(2433, confs.get(TOT_MULTI_BATCH).getUpperBound());
    assertEquals(1573, confs.get(TOT_MULTI_BATCH).getLowerBound());
  }
}
