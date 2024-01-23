package com.mercadolibre.planning.model.api.web.controller.suggestionwaves;

import static com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.ResponseMapper.mapToDto;

import com.mercadolibre.planning.model.api.domain.entity.ProcessPath;
import com.mercadolibre.planning.model.api.projection.ProcessPathConfiguration;
import com.mercadolibre.planning.model.api.projection.UnitsByProcessPathAndProcess;
import com.mercadolibre.planning.model.api.projection.waverless.WavesCalculator;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.request.Request;
import com.mercadolibre.planning.model.api.web.controller.suggestionwaves.response.WaverlessResponse;
import com.newrelic.api.agent.Trace;
import java.util.List;
import java.util.Set;
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


  @Trace(dispatcher = true)
  @PostMapping("/waves")
  public ResponseEntity<WaverlessResponse> getSuggestions(
      @PathVariable final String logisticCenterId,
      @RequestBody final Request request
  ) {

    final Set<ProcessPath> processPath = request.getProcessPathConfigurations()
        .stream()
        .map(ProcessPathConfiguration::getProcessPath)
        .collect(Collectors.toSet());

    final var waves = WavesCalculator.waves(
        request.getViewDate(),
        request.getProcessPathConfigurations(),
        getBacklogFiltered(request.getBacklogs(), processPath),
        List.of(),
        request.getIntThroughput(),
        request.getPrecalculatedWavesAsEntities(),
        logisticCenterId,
        request.getWaveSizeConfig()
    );
    return ResponseEntity.ok(mapToDto(logisticCenterId, request.getViewDate(), waves));
  }

  private List<UnitsByProcessPathAndProcess> getBacklogFiltered(final List<UnitsByProcessPathAndProcess> backlog,
                                                                final Set<ProcessPath> processPaths) {
    return backlog.stream()
        .filter(processPathAndProcess -> processPaths.contains(processPathAndProcess.getProcessPath()))
        .toList();
  }

}
