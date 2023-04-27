package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.BATCH_SORTER;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WALL_IN;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.TPH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog;
import com.mercadolibre.planning.model.api.projection.waverless.PendingBacklog.AvailableBacklog;
import com.mercadolibre.planning.model.api.util.DateUtils;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UpperBoundsCalculatorTest {

  private static final List<Instant> INFLECTION_POINTS = DateUtils.generateInflectionPoints(
      Instant.parse("2023-03-06T00:00:00Z"),
      Instant.parse("2023-03-06T05:00:00Z"),
      5
  );

  private static final Instant WAVE_EXECUTION_DATE = Instant.parse("2023-03-06T01:15:00Z");

  private static final Map<Instant, Integer> HIGH_LIMITS = tph(10000);

  private static final Map<Instant, Integer> LOW_LIMITS = tph(5000);

  private static final Map<Instant, Integer> ZERO_LIMITS = tph(0);

  private static final Map<ProcessName, Map<Instant, Long>> PROJECTED_BACKLOG = Map.of(
      PICKING, Map.of(WAVE_EXECUTION_DATE, 1500L)
  );

  private static final PendingBacklog PENDING_BACKLOG = new PendingBacklog(
      Map.of(
          TOT_MONO, List.of(availableBacklog(10000D)),
          NON_TOT_MONO, List.of(availableBacklog(10000D)),
          TOT_MULTI_BATCH, List.of(availableBacklog(10000D))
      ),
      Collections.emptyMap()
  );

  private static final Map<ProcessName, Map<ProcessPath, Map<Instant, Long>>> CURRENT_BACKLOG = Map.of(
      PICKING, Map.of(
          TOT_MONO, backlog(2500L),
          NON_TOT_MONO, backlog(1500L),
          TOT_MULTI_BATCH, backlog(2000L)
      ),
      PACKING, Map.of(
          TOT_MONO, backlog(3000L),
          NON_TOT_MONO, backlog(800L)
      ),
      BATCH_SORTER, Map.of(
          TOT_MULTI_BATCH, backlog(1780L)
      ),
      WALL_IN, Map.of(
          TOT_MULTI_BATCH, backlog(490L)
      ),
      PACKING_WALL, Map.of(
          TOT_MULTI_BATCH, backlog(800L)
      )
  );

  private static final Map<ProcessName, Map<Instant, Integer>> THROUGHPUT = Map.of(
      PICKING, tph(300 * 12),
      PACKING, tph(150 * 12),
      BATCH_SORTER, tph(130 * 12),
      WALL_IN, tph(40 * 12),
      PACKING_WALL, tph(80 * 12)
  );

  private static final Map<ProcessPath, Map<Instant, Integer>> PICKING_THROUGHPUT = Map.of(
      TOT_MONO, TPH,
      NON_TOT_MONO, TPH,
      TOT_MULTI_BATCH, TPH
  );

  private static Map<Instant, Integer> tph(int val) {
    return Map.of(
        Instant.parse("2023-03-06T00:00:00Z"), val,
        Instant.parse("2023-03-06T01:00:00Z"), val,
        Instant.parse("2023-03-06T02:00:00Z"), val,
        Instant.parse("2023-03-06T03:00:00Z"), val,
        Instant.parse("2023-03-06T04:00:00Z"), val,
        Instant.parse("2023-03-06T05:00:00Z"), val
    );
  }

  private static AvailableBacklog availableBacklog(final double quantity) {
    return new AvailableBacklog(WAVE_EXECUTION_DATE, WAVE_EXECUTION_DATE, quantity);
  }

  private static Map<Instant, Long> backlog(final long quantity) {
    return Map.of(WAVE_EXECUTION_DATE, quantity);
  }

  @Test
  @DisplayName("on trigger projection with equals tph and without limits then process path wave sizes must be equal")
  void test() {
    // GIVEN
    final var limits = Map.of(
        PICKING, LOW_LIMITS,
        PACKING, HIGH_LIMITS,
        BATCH_SORTER, HIGH_LIMITS,
        WALL_IN, HIGH_LIMITS,
        PACKING_WALL, HIGH_LIMITS
    );

    // WHEN
    final var result = UpperBoundsCalculator.calculate(
        WAVE_EXECUTION_DATE,
        INFLECTION_POINTS,
        Collections.emptyList(),
        PENDING_BACKLOG,
        CURRENT_BACKLOG,
        PROJECTED_BACKLOG,
        THROUGHPUT,
        PICKING_THROUGHPUT,
        limits
    );

    // THEN
    assertNotNull(result);
    assertEquals(3, result.size());

    // (5000 - 1200) / 3
    assertEquals(1266, result.get(TOT_MONO));
    assertEquals(1266, result.get(NON_TOT_MONO));
    assertEquals(1266, result.get(TOT_MULTI_BATCH));
  }

  @Test
  @DisplayName("on trigger projection without a small packing buffer available then packing process paths must be capped")
  void testCappedByPacking() {
    // GIVEN
    final var limits = Map.of(
        PICKING, LOW_LIMITS,
        PACKING, LOW_LIMITS,
        BATCH_SORTER, HIGH_LIMITS,
        WALL_IN, HIGH_LIMITS,
        PACKING_WALL, HIGH_LIMITS
    );

    // WHEN
    final var result = UpperBoundsCalculator.calculate(
        WAVE_EXECUTION_DATE,
        INFLECTION_POINTS,
        Collections.emptyList(),
        PENDING_BACKLOG,
        CURRENT_BACKLOG,
        PROJECTED_BACKLOG,
        THROUGHPUT,
        PICKING_THROUGHPUT,
        limits
    );

    // THEN
    assertNotNull(result);
    assertEquals(3, result.size());

    assertEquals(1105, result.get(TOT_MONO));
    assertEquals(1105, result.get(NON_TOT_MONO));
    assertEquals(1587, result.get(TOT_MULTI_BATCH));
  }

  @Test
  @DisplayName("on trigger projection without a small packing buffer available then packing process paths must be capped")
  void testCappedByPackingAndPendingBacklog() {
    // GIVEN
    final var limits = Map.of(
        PICKING, LOW_LIMITS,
        PACKING, LOW_LIMITS,
        BATCH_SORTER, HIGH_LIMITS,
        WALL_IN, HIGH_LIMITS,
        PACKING_WALL, HIGH_LIMITS
    );

    final PendingBacklog pendingBacklog = new PendingBacklog(
        Map.of(
            TOT_MONO, List.of(availableBacklog(10000D)),
            NON_TOT_MONO, List.of(availableBacklog(10000D)),
            TOT_MULTI_BATCH, List.of(availableBacklog(1000D))
        ),
        Collections.emptyMap()
    );

    // WHEN
    final var result = UpperBoundsCalculator.calculate(
        WAVE_EXECUTION_DATE,
        INFLECTION_POINTS,
        Collections.emptyList(),
        pendingBacklog,
        CURRENT_BACKLOG,
        PROJECTED_BACKLOG,
        THROUGHPUT,
        PICKING_THROUGHPUT,
        limits
    );

    // THEN
    assertNotNull(result);
    assertEquals(3, result.size());

    assertEquals(869, result.get(TOT_MONO));
    assertEquals(869, result.get(NON_TOT_MONO));
    assertEquals(1000, result.get(TOT_MULTI_BATCH));
  }

  @Test
  @DisplayName("on trigger projection without available buffer in packing and packing wall then wave size must be zero")
  void testFullyCappedProcesses() {
    // GIVEN
    final var limits = Map.of(
        PICKING, HIGH_LIMITS,
        PACKING, ZERO_LIMITS,
        BATCH_SORTER, HIGH_LIMITS,
        WALL_IN, HIGH_LIMITS,
        PACKING_WALL, ZERO_LIMITS
    );

    final PendingBacklog pendingBacklog = new PendingBacklog(
        Map.of(
            TOT_MONO, List.of(availableBacklog(10000D)),
            NON_TOT_MONO, List.of(availableBacklog(10000D)),
            TOT_MULTI_BATCH, List.of(availableBacklog(1000D))
        ),
        Collections.emptyMap()
    );

    // WHEN
    final var result = UpperBoundsCalculator.calculate(
        WAVE_EXECUTION_DATE,
        INFLECTION_POINTS,
        Collections.emptyList(),
        pendingBacklog,
        CURRENT_BACKLOG,
        PROJECTED_BACKLOG,
        THROUGHPUT,
        PICKING_THROUGHPUT,
        limits
    );

    // THEN
    assertNotNull(result);
    assertEquals(3, result.size());

    assertEquals(0, result.get(TOT_MONO));
    assertEquals(0, result.get(NON_TOT_MONO));
    assertEquals(0, result.get(TOT_MULTI_BATCH));
  }

  @Test
  @DisplayName("on trigger projection without available buffer in packing and packing wall then wave size must be zero")
  void testFullyCappedByPacking() {
    // GIVEN
    final var limits = Map.of(
        PICKING, LOW_LIMITS,
        PACKING, ZERO_LIMITS,
        BATCH_SORTER, HIGH_LIMITS,
        WALL_IN, HIGH_LIMITS,
        PACKING_WALL, HIGH_LIMITS
    );

    final PendingBacklog pendingBacklog = new PendingBacklog(
        Map.of(
            TOT_MONO, List.of(availableBacklog(10000D)),
            NON_TOT_MONO, List.of(availableBacklog(10000D)),
            TOT_MULTI_BATCH, List.of(availableBacklog(1000D))
        ),
        Collections.emptyMap()
    );

    // WHEN
    final var result = UpperBoundsCalculator.calculate(
        WAVE_EXECUTION_DATE,
        INFLECTION_POINTS,
        Collections.emptyList(),
        pendingBacklog,
        CURRENT_BACKLOG,
        PROJECTED_BACKLOG,
        THROUGHPUT,
        PICKING_THROUGHPUT,
        limits
    );

    // THEN
    assertNotNull(result);
    assertEquals(3, result.size());

    assertEquals(0, result.get(TOT_MONO));
    assertEquals(0, result.get(NON_TOT_MONO));
    assertEquals(1000, result.get(TOT_MULTI_BATCH));
  }

}
