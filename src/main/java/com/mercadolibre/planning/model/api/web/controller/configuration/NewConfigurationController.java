package com.mercadolibre.planning.model.api.web.controller.configuration;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.usecase.configuration.ConfigurationUseCase;
import com.mercadolibre.planning.model.api.web.controller.configuration.request.ConfigurationRequestDto;
import com.mercadolibre.planning.model.api.web.controller.configuration.response.ConfigurationResponseDto;
import com.newrelic.api.agent.Trace;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/logistic_center/{logisticCenterId}/configuration")
public class NewConfigurationController {

  private final ConfigurationUseCase configurationUseCase;

  @Trace(dispatcher = true)
  @PostMapping
  public ResponseEntity<List<ConfigurationResponseDto>> save(
      @PathVariable final String logisticCenterId,
      @RequestParam final long userId,
      @RequestBody @Valid final List<ConfigurationRequestDto> configurationRequests) {

    final Map<String, String> configurationsByKey = configurationRequests.stream()
        .collect(toMap(ConfigurationRequestDto::key, ConfigurationRequestDto::value));

    return ResponseEntity.ok(
        configurationUseCase.save(userId, logisticCenterId, configurationsByKey)
            .stream()
            .map(config -> new ConfigurationResponseDto(config.getKey(), config.getValue()))
            .toList()
    );
  }

}
