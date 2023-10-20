package com.mercadolibre.planning.model.api.web.controller.configuration;

import static java.util.stream.Collectors.toMap;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.ConfigurationUseCase;
import com.newrelic.api.agent.Trace;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/logistic_center/{logisticCenterId}/configuration")
public class NewConfigurationController {

  private final ConfigurationUseCase configurationUseCase;

  @Trace(dispatcher = true)
  @PostMapping
  public ResponseEntity<Map<String, String>> save(
      @PathVariable final String logisticCenterId,
      @RequestParam final long userId,
      @RequestBody @Valid final Map<@NotBlank String, @NotBlank String> configurations) {

    return ResponseEntity.ok(
        configurationUseCase.save(userId, logisticCenterId, configurations)
            .stream()
            .collect(
                toMap(
                    Configuration::getKey,
                    Configuration::getValue,
                    (o1, o2) -> o2
                )
            )
    );
  }

}
