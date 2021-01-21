package com.mercadolibre.planning.model.api.domain.usecase.configuration.update;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.configuration.ConfigurationId;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.ConfigurationInput;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UpdateConfigurationUseCase
        implements UseCase<ConfigurationInput, Configuration> {

    private final ConfigurationRepository configurationRepository;

    @Override
    public Configuration execute(final ConfigurationInput input) {
        final String logisticCenterId = input.getLogisticCenterId();
        final String key = input.getKey();

        final String configurationId = logisticCenterId + "-" + key;
        final Configuration savedConfiguration = configurationRepository.findById(
                new ConfigurationId(logisticCenterId, key))
                .orElseThrow(() -> new EntityNotFoundException("CONFIGURATION", configurationId));

        savedConfiguration.setValue(input.getValue());
        savedConfiguration.setMetricUnit(input.getMetricUnit());

        return configurationRepository.save(savedConfiguration);
    }
}

