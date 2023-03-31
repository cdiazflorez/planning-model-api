package com.mercadolibre.planning.model.api.projection.waverless;

import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_1;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_2;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.SLA_3;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.THROUGHPUT;
import static com.mercadolibre.planning.model.api.projection.waverless.WavesBySlaUtil.inflectionPoint;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mercadolibre.planning.model.api.projection.ProcessPathConfiguration;
import com.mercadolibre.planning.model.api.projection.UnitsByProcessPathAndProcess;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WaveCalculatorTest {
  private static final List<UnitsByProcessPathAndProcess> BACKLOG = List.of(
      new UnitsByProcessPathAndProcess(TOT_MONO, WAVING, SLA_1, 5),
      new UnitsByProcessPathAndProcess(TOT_MONO, WAVING, SLA_2, 110),
      new UnitsByProcessPathAndProcess(TOT_MONO, WAVING, SLA_3, 1),
      new UnitsByProcessPathAndProcess(TOT_MONO, PICKING, SLA_1, 17)
  );

  private static final List<ForecastedUnitsByProcessPath> FORECAST = List.of(
      new ForecastedUnitsByProcessPath(TOT_MONO, inflectionPoint(0), SLA_2, 60)
  );

  private static final List<ProcessPathConfiguration> CONFIGURATIONS = List.of(new ProcessPathConfiguration(TOT_MONO, 120, 100, 60));

  @Test
  @DisplayName("on wave calculation, return several waves")
  void testSearchWaves() {
    // GIVEN

    // WHEN
    final var result = WavesCalculator.waves(
        inflectionPoint(0),
        CONFIGURATIONS,
        BACKLOG,
        FORECAST,
        THROUGHPUT
    );

    // THEN
    assertNotNull(result);
    assertEquals(6, result.size());
  }

  @Test
  @DisplayName("on empty inputs, return no waves")
  void onEmptyInputsThenReturnNoWaves() {
    // GIVEN

    // WHEN
    final var result = WavesCalculator.waves(
        inflectionPoint(0),
        CONFIGURATIONS,
        Collections.emptyList(),
        Collections.emptyList(),
        THROUGHPUT
    );

    // THEN
    assertTrue(result.isEmpty());
  }
}
