package com.mercadolibre.planning.model.api.web.controller.suggestionwaves;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.waverless.Wave;
import com.mercadolibre.planning.model.api.projection.waverless.Wave.WaveConfiguration;
import com.mercadolibre.planning.model.api.projection.waverless.WavesCalculator;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.request.Request;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.WaveConfigurationDto;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.WaveDto;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.WaverlessResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/logistic_center/{logisticCenterId}/projections")
public class SuggestionWavesController {

  private static Map<ProcessPath, Map<Instant, Integer>> asIntThroughput(final Map<ProcessPath, Map<Instant, Float>> throughput) {
    return throughput.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> entry.getValue().entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, tph -> tph.getValue().intValue()))
        ));
  }

  private static WaveConfigurationDto toWave(final ProcessPath processPath, final WaveConfiguration configuration) {
    return new WaveConfigurationDto(
        processPath,
        (int) configuration.getLowerBound(),
        (int) configuration.getUpperBound(),
        new TreeSet<>(configuration.getWavedUnitsByCpt().keySet())
    );
  }

  private static List<WaveConfigurationDto> toWaves(final Map<ProcessPath, WaveConfiguration> configuration) {
    return configuration.entrySet()
        .stream()
        .map(conf -> toWave(conf.getKey(), conf.getValue()))
        .collect(Collectors.toList());
  }

  private static WaverlessResponse mapToDto(final String logisticCenterId, final Instant viewDate, final List<Wave> waves) {
    final var suggestions = waves.stream()
        .map(wave -> new WaveDto(wave.getDate(), toWaves(wave.getConfiguration()), wave.getReason()))
        .collect(Collectors.toList());

    return new WaverlessResponse(logisticCenterId, viewDate, suggestions);
  }

  @PostMapping("/waves")
  public ResponseEntity<WaverlessResponse> getSuggestions(
      @PathVariable final String logisticCenterId,
      final @RequestBody Request request
  ) {
    final var waves = WavesCalculator.waves(
        request.getViewDate(),
        request.getProcessPathConfigurations(),
        request.getBacklogs(),
        request.getForecast(),
        asIntThroughput(request.getThroughput())
    );
    return ResponseEntity.ok(mapToDto(logisticCenterId, request.getViewDate(), waves));
  }
}
