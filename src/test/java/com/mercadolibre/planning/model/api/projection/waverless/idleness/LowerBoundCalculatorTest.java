package com.mercadolibre.planning.model.api.projection.waverless.idleness;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class LowerBoundCalculatorTest {

  private static final String HOUR_3 = "2023-04-12T03:00:00Z";

  private static final String HOUR_4 = "2023-04-12T04:00:00Z";

  private static final String HOUR_5 = "2023-04-12T05:00:00Z";

  private static Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> getThroughputByProcessPath() {
    final Map<Instant, Integer> mapTphCurrentAndNext = Map.of(
        Instant.parse(HOUR_3), 100,
        Instant.parse(HOUR_4), 200,
        Instant.parse("2023-04-12T07:00:00Z"), 80,
        Instant.parse("2023-04-12T09:00:00Z"), 100
    );

    final Map<Instant, Integer> mapTphOnlyCurrent = Map.of(
        Instant.parse(HOUR_3), 100,
        Instant.parse("2023-04-12T07:00:00Z"), 80
    );

    final Map<Instant, Integer> mapTphOnlyNext = Map.of(
        Instant.parse(HOUR_4), 200,
        Instant.parse("2023-04-12T09:00:00Z"), 100
    );

    final Map<Instant, Integer> mapTphWithoutCurrentOrNext = Map.of(
        Instant.parse("2023-04-12T07:00:00Z"), 80,
        Instant.parse("2023-04-12T09:00:00Z"), 100
    );


    final Map<ProcessName, Map<Instant, Integer>> mapForTotMono = Map.of(
        ProcessName.PICKING, mapTphCurrentAndNext,
        ProcessName.PACKING, mapTphCurrentAndNext
    );

    final Map<ProcessName, Map<Instant, Integer>> mapForNonTotMono = Map.of(
        ProcessName.PICKING, mapTphWithoutCurrentOrNext,
        ProcessName.PACKING, mapTphCurrentAndNext
    );

    final Map<ProcessName, Map<Instant, Integer>> mapForTotMultiOrder = Map.of(
        ProcessName.PICKING, mapTphOnlyCurrent,
        ProcessName.PACKING, mapTphCurrentAndNext
    );

    final Map<ProcessName, Map<Instant, Integer>> mapForTotMultiBatch = Map.of(
        ProcessName.PICKING, mapTphOnlyNext,
        ProcessName.PACKING, mapTphCurrentAndNext
    );

    final Map<ProcessName, Map<Instant, Integer>> mapForBulky = Map.of(
        ProcessName.PACKING, mapTphCurrentAndNext
    );


    return Map.of(
        ProcessPath.TOT_MONO, mapForTotMono,
        ProcessPath.NON_TOT_MONO, mapForNonTotMono,
        ProcessPath.TOT_MULTI_ORDER, mapForTotMultiOrder,
        ProcessPath.TOT_MULTI_BATCH, mapForTotMultiBatch,
        ProcessPath.GLOBAL, mapForTotMono,
        ProcessPath.BULKY, mapForBulky
    );
  }

  @Test
  void testLowerBounds() {
    Map<ProcessPath, Integer> lowerBoundsInCurrentAndNextHour =
        LowerBoundCalculator.lowerBounds(30, Instant.parse("2023-04-12T03:50:00Z"), getThroughputByProcessPath());
    Map<ProcessPath, Integer> lowerBoundsInCurrentHour =
        LowerBoundCalculator.lowerBounds(30, Instant.parse("2023-04-12T03:10:00Z"), getThroughputByProcessPath());

    Assertions.assertEquals(82, lowerBoundsInCurrentAndNextHour.get(ProcessPath.TOT_MONO));
    Assertions.assertEquals(0, lowerBoundsInCurrentAndNextHour.get(ProcessPath.NON_TOT_MONO));
    Assertions.assertEquals(16, lowerBoundsInCurrentAndNextHour.get(ProcessPath.TOT_MULTI_ORDER));
    Assertions.assertEquals(66, lowerBoundsInCurrentAndNextHour.get(ProcessPath.TOT_MULTI_BATCH));
    Assertions.assertEquals(0, lowerBoundsInCurrentAndNextHour.get(ProcessPath.BULKY));
    Assertions.assertFalse(lowerBoundsInCurrentAndNextHour.containsKey(ProcessPath.GLOBAL));

    Assertions.assertEquals(50, lowerBoundsInCurrentHour.get(ProcessPath.TOT_MONO));
    Assertions.assertEquals(0, lowerBoundsInCurrentHour.get(ProcessPath.NON_TOT_MONO));
    Assertions.assertEquals(50, lowerBoundsInCurrentHour.get(ProcessPath.TOT_MULTI_ORDER));
    Assertions.assertEquals(0, lowerBoundsInCurrentHour.get(ProcessPath.TOT_MULTI_BATCH));
    Assertions.assertEquals(0, lowerBoundsInCurrentHour.get(ProcessPath.BULKY));
    Assertions.assertFalse(lowerBoundsInCurrentHour.containsKey(ProcessPath.GLOBAL));
  }

  @Test
  void testLowerBoundsBetweenSeveralHours() {
    final Map<Instant, Integer> mapTph1 = Map.of(
        Instant.parse(HOUR_3), 100,
        Instant.parse(HOUR_4), 200,
        Instant.parse(HOUR_5), 80,
        Instant.parse("2023-04-12T06:00:00Z"), 100
    );

    final Map<Instant, Integer> mapTph2 = Map.of(
        Instant.parse(HOUR_3), 100,
        Instant.parse(HOUR_5), 80,
        Instant.parse("2023-04-12T06:00:00Z"), 100
    );

    final Map<ProcessName, Map<Instant, Integer>> mapProcessName1 = Map.of(
        ProcessName.PICKING, mapTph1
    );

    final Map<ProcessName, Map<Instant, Integer>> mapProcessName2 = Map.of(
        ProcessName.PICKING, mapTph2
    );

    final Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> mapByProcessPath = Map.of(
        ProcessPath.TOT_MONO, mapProcessName1,
        ProcessPath.NON_TOT_MONO, mapProcessName2
    );

    Map<ProcessPath, Integer> lowerBoundsBetweenSeveralHours =
        LowerBoundCalculator.lowerBounds(90, Instant.parse("2023-04-12T03:50:00Z"), mapByProcessPath);

    Assertions.assertEquals(242, lowerBoundsBetweenSeveralHours.get(ProcessPath.TOT_MONO));
    Assertions.assertEquals(42, lowerBoundsBetweenSeveralHours.get(ProcessPath.NON_TOT_MONO));
  }

  @Test
  void testLowerBoundsExactToEndHour() {
    final Map<Instant, Integer> mapTph1 = Map.of(
        Instant.parse(HOUR_3), 100,
        Instant.parse(HOUR_4), 200
    );

    final Map<ProcessName, Map<Instant, Integer>> mapProcessName1 = Map.of(
        ProcessName.PICKING, mapTph1
    );

    final Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> mapByProcessPath = Map.of(
        ProcessPath.TOT_MONO, mapProcessName1
    );

    Map<ProcessPath, Integer> lowerBoundsExactToEndHour1 =
        LowerBoundCalculator.lowerBounds(30, Instant.parse("2023-04-12T03:30:00Z"), mapByProcessPath);
    Map<ProcessPath, Integer> lowerBoundsExactToEndHour2 =
        LowerBoundCalculator.lowerBounds(60, Instant.parse(HOUR_4), mapByProcessPath);

    Assertions.assertEquals(50, lowerBoundsExactToEndHour1.get(ProcessPath.TOT_MONO));
    Assertions.assertEquals(200, lowerBoundsExactToEndHour2.get(ProcessPath.TOT_MONO));
  }

}
