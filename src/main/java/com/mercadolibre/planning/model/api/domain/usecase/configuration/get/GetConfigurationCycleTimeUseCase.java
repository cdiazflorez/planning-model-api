package com.mercadolibre.planning.model.api.domain.usecase.configuration.get;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class GetConfigurationCycleTimeUseCase implements UseCase<String, List<Configuration>> {

    private final ConfigurationRepository configurationRepository;

    @Override
    public List<Configuration> execute(final String warehouseId) {
        return configurationRepository.findByWarehouseId(warehouseId);
    }
}
