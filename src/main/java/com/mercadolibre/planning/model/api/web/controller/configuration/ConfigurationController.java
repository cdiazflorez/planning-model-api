package com.mercadolibre.planning.model.api.web.controller.configuration;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.GetConfigurationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetConfigurationInput;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/configuration")
public class ConfigurationController {

    private final GetConfigurationUseCase getConfiguration;

    @GetMapping
    public ResponseEntity<ConfigurationResponse> get(@RequestParam final String logisticCenterId,
                                                     @RequestParam final String key) {
        return ResponseEntity.ok(toResponse(
                getConfiguration.execute(new GetConfigurationInput(logisticCenterId, key))
                        .orElseThrow(() -> new EntityNotFoundException(
                                "CONFIGURATION",
                                logisticCenterId + key)
                        )
        ));
    }

    private ConfigurationResponse toResponse(final Configuration configuration) {
        return new ConfigurationResponse(
                configuration.getValue(),
                configuration.getMetricUnit().toJson());
    }

}
