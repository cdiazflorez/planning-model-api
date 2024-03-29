package com.mercadolibre.planning.model.api.domain.usecase.configuration.get;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.configuration.ConfigurationId;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@AllArgsConstructor
public class GetConfigurationByKeyUseCase implements
        UseCase<GetConfigurationInput, Optional<Configuration>> {

    private final ConfigurationRepository configurationRepository;

    @Override
    public Optional<Configuration> execute(final GetConfigurationInput input) {
        return configurationRepository.findById(new ConfigurationId(
                input.getLogisticCenterId(),
                input.getKey())
        );
    }
}
