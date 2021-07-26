package com.mercadolibre.planning.model.api.web.controller.configuration;

import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.CreateConfigurationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationByKeyUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.update.UpdateConfigurationUseCase;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import com.mercadolibre.planning.model.api.web.controller.configuration.request.CreateConfigurationRequest;
import com.mercadolibre.planning.model.api.web.controller.configuration.request.UpdateConfigurationRequest;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@AllArgsConstructor
@RequestMapping("/planning/model/configuration")
public class ConfigurationController {

    private final GetConfigurationByKeyUseCase getConfiguration;
    private final CreateConfigurationUseCase createConfigurationUseCase;
    private final UpdateConfigurationUseCase updateConfigurationUseCase;

    @GetMapping
    @Trace(dispatcher = true)
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

    @PostMapping
    @Trace(dispatcher = true)
    public ResponseEntity<ConfigurationResponse> create(
            @RequestBody @Valid final CreateConfigurationRequest request) {

        return ResponseEntity.ok(toResponse(
                createConfigurationUseCase.execute(request.toConfigurationInput())
        ));
    }

    @PutMapping("/{logisticCenterId}/{key}")
    @Trace(dispatcher = true)
    public ResponseEntity<ConfigurationResponse> update(
            @PathVariable final String logisticCenterId,
            @PathVariable final String key,
            @RequestBody @Valid final UpdateConfigurationRequest request) {

        return ResponseEntity.ok(
                toResponse(
                        updateConfigurationUseCase.execute(
                                request.toConfigurationInput(logisticCenterId, key))));
    }

    private ConfigurationResponse toResponse(final Configuration configuration) {
        return new ConfigurationResponse(
                configuration.getValue(),
                configuration.getMetricUnit().toJson());
    }
}
