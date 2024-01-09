package com.mercadolibre.planning.model.api.web.controller.suggestionwaves;

import static com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.ResponseMapper.mapToDto;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.domain.entity.TriggerName;
import com.mercadolibre.planning.model.api.projection.waverless.Wave;
import com.mercadolibre.planning.model.api.projection.waverless.WavesCalculator;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.WaverlessResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ResponseMapperTest {

  private static final String WH = "ARBA01";
  private static final Instant VIEW_DATE = Instant.parse("2023-10-13T00:00:00Z");

  @Test
  void mapToDtoTest() {

    WaverlessResponse response = mapToDto(WH, VIEW_DATE, dataTrigger());
    assertNotNull(response);
    assertEquals(2, response.getSuggestions().size());
    assertEquals(5, response.getSuggestions().get(0).getWaves().size());


  }

  private WavesCalculator.TriggerProjection dataTrigger() {
    return new WavesCalculator.TriggerProjection(
        List.of(
            new Wave(
                VIEW_DATE,
                TriggerName.SLA,
                Map.of(ProcessPath.NON_TOT_MONO,
                    new Wave.WaveConfiguration(
                        45,
                        -1,
                        Map.of(VIEW_DATE.plus(1, ChronoUnit.HOURS), 5L,
                            VIEW_DATE.plus(2, ChronoUnit.HOURS), 5L,
                            VIEW_DATE.plus(3, ChronoUnit.HOURS), 5L,
                            VIEW_DATE.plus(4, ChronoUnit.HOURS), 5L,
                            VIEW_DATE.plus(270, ChronoUnit.MINUTES), 5L,
                            VIEW_DATE.plus(280, ChronoUnit.MINUTES), 5L,
                            VIEW_DATE.plus(290, ChronoUnit.MINUTES), 5L,
                            VIEW_DATE.plus(5, ChronoUnit.HOURS), 5L,
                            VIEW_DATE.plus(7, ChronoUnit.HOURS), 5L,
                            VIEW_DATE.plus(8, ChronoUnit.HOURS), 5L
                        )
                    )
                )
            ),
            new Wave(
                VIEW_DATE,
                TriggerName.IDLENESS,
                Map.of(ProcessPath.TOT_MONO,
                    new Wave.WaveConfiguration(
                        250,
                        300,
                        Map.of(VIEW_DATE.plus(1, ChronoUnit.HOURS), 50L,
                            VIEW_DATE.plus(2, ChronoUnit.HOURS), 50L,
                            VIEW_DATE.plus(3, ChronoUnit.HOURS), 50L,
                            VIEW_DATE.plus(4, ChronoUnit.HOURS), 50L,
                            VIEW_DATE.plus(5, ChronoUnit.HOURS), 50L)
                    )
                )
            )
        ),
        Map.of()
    );
  }
}
