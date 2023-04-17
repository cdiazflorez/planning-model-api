package com.mercadolibre.planning.model.api.web.controller.suggestionwaves.request;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.ProcessPathConfiguration;
import com.mercadolibre.planning.model.api.projection.UnitsByProcessPathAndProcess;
import com.mercadolibre.planning.model.api.projection.waverless.BacklogLimits;
import com.mercadolibre.planning.model.api.projection.waverless.ForecastedUnitsByProcessPath;
import com.mercadolibre.planning.model.api.projection.waverless.PrecalculatedWave;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Value;

@Value
public class Request {
  Instant viewDate;

  List<ProcessPathConfiguration> processPathConfigurations;

  List<UnitsByProcessPathAndProcess> backlogs;

  List<ForecastedUnitsByProcessPath> forecast;

  Map<ProcessPath, Map<ProcessName, Map<Instant, Float>>> throughput;

  BacklogLimits backlogLimits;

  Map<ProcessPath, List<PrecalculatedWaveDto>> precalculatedWaves;

  private static Map<ProcessName, Map<Instant, Integer>> mapProcessNamesTph(final Map<ProcessName, Map<Instant, Float>> tph) {
    final Function<Map<Instant, Float>, Map<Instant, Integer>> asIntMap = map -> map.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().intValue()));

    return tph.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> asIntMap.apply(entry.getValue())));
  }

  private static List<PrecalculatedWave> asPrecalculatedWaveEntity(final List<PrecalculatedWaveDto> dtos) {
    return dtos.stream()
        .map(pw -> new PrecalculatedWave(pw.getUnitsBySla()))
        .collect(Collectors.toList());
  }

  public Map<ProcessPath, Map<ProcessName, Map<Instant, Integer>>> getIntThroughput() {
    return throughput.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> mapProcessNamesTph(entry.getValue())));
  }

  public Map<ProcessPath, List<PrecalculatedWave>> getPrecalculatedWavesAsEntities() {
    if (precalculatedWaves == null) {
      return Collections.emptyMap();
    }
    return precalculatedWaves.entrySet()
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, entry -> asPrecalculatedWaveEntity(entry.getValue())));
  }

  @Data
  public static class PrecalculatedWaveDto {
    Map<Instant, Integer> unitsBySla;
  }

}
