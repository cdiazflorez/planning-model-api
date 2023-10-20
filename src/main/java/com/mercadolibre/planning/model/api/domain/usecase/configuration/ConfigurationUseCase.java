package com.mercadolibre.planning.model.api.domain.usecase.configuration;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.NA;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConfigurationUseCase {

  private final ConfigurationRepository configurationRepository;

  public List<Configuration> save(final long userId,
                                  final String logisticCenterId,
                                  final Map<String, String> configByKey) {
    final List<Configuration> configurations = configByKey.keySet().stream()
        .map(key -> Configuration.builder()
            .logisticCenterId(logisticCenterId)
            .key(key)
            .value(configByKey.get(key))
            .metricUnit(NA)
            .lastUserUpdated(userId)
            .build())
        .toList();
    return configurationRepository.saveAll(configurations);
  }

}
