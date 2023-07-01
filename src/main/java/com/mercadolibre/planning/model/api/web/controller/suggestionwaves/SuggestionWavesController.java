package com.mercadolibre.planning.model.api.web.controller.suggestionwaves;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.waverless.Wave.WaveConfiguration;
import com.mercadolibre.planning.model.api.projection.waverless.WavesCalculator;
import com.mercadolibre.planning.model.api.projection.waverless.WavesCalculator.TriggerProjection;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.request.Request;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.WaveConfigurationDto;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.WaveConfigurationDto.UnitsAtSla;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.WaveDto;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.WaverlessResponse;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.WaverlessResponse.UnitsAtOperationHour;
import com.newrelic.api.agent.Trace;
import java.time.Instant;
import java.util.Comparator;
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

  private static WaveConfigurationDto toWave(final ProcessPath processPath, final WaveConfiguration configuration) {
    return new WaveConfigurationDto(
        processPath,
        (int) configuration.getLowerBound(),
        (int) configuration.getUpperBound(),
        new TreeSet<>(configuration.getWavedUnitsByCpt().keySet()),
        configuration.getWavedUnitsByCpt()
            .entrySet()
            .stream()
            .map(entry -> new UnitsAtSla(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(UnitsAtSla::getSla))
            .collect(Collectors.toList())
    );
  }

  private static List<WaveConfigurationDto> toWaves(final Map<ProcessPath, WaveConfiguration> configuration) {
    return configuration.entrySet()
        .stream()
        .map(conf -> toWave(conf.getKey(), conf.getValue()))
        .collect(Collectors.toList());
  }

  private static WaverlessResponse mapToDto(final String logisticCenterId, final Instant viewDate, final TriggerProjection triggers) {
    final var suggestions = triggers.getWaves().stream()
        .map(wave -> new WaveDto(wave.getDate(), toWaves(wave.getConfiguration()), wave.getReason()))
        .collect(Collectors.toList());

    final var projectedBacklogs = triggers.getProjectedBacklogs()
        .entrySet()
        .stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue()
                    .entrySet()
                    .stream()
                    .map(unitsByDate -> new UnitsAtOperationHour(unitsByDate.getKey(), unitsByDate.getValue()))
                    .sorted(Comparator.comparing(UnitsAtOperationHour::getDate))
                    .collect(Collectors.toList())
            )
        );

    return new WaverlessResponse(logisticCenterId, viewDate, suggestions, projectedBacklogs);
  }

  @Trace(dispatcher = true)
  @PostMapping("/waves")
  public ResponseEntity<WaverlessResponse> getSuggestions(
      @PathVariable final String logisticCenterId,
      @RequestBody final Request request
  ) {
    final var waves = WavesCalculator.waves(
        request.getViewDate(),
        request.getProcessPathConfigurations(),
        request.getBacklogs(),
        request.getForecast(),
        request.getIntThroughput(),
        request.getBacklogLimits(),
        request.getPrecalculatedWavesAsEntities()
    );
    return ResponseEntity.ok(mapToDto(logisticCenterId, request.getViewDate(), waves));
  }

}
