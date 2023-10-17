package com.mercadolibre.planning.model.api.domain.usecase.configuration;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.NA;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.web.controller.configuration.request.ConfigurationRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ConfigurationUseCase {

  private final ConfigurationRepository configurationRepository;

  public List<Configuration> upsert(final long userId,
                                    final String logisticCenterId,
                                    final List<ConfigurationRequest> configurationRequests) {
    final List<Configuration> configurations = configurationRequests.stream()
        .map(config -> Configuration.builder()
            .logisticCenterId(logisticCenterId)
            .key(config.key())
            .value(config.value())
            .metricUnit(NA)
            .lastUserUpdated(userId)
            .build())
        .toList();
    return configurationRepository.saveAll(configurations);
  }

}
