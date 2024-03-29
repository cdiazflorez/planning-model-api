package com.mercadolibre.planning.model.api.domain.usecase.configuration.create;

import static java.lang.String.format;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.configuration.ConfigurationId;
import com.mercadolibre.planning.model.api.domain.usecase.UseCase;
import com.mercadolibre.planning.model.api.exception.EntityAlreadyExistsException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CreateConfigurationUseCase
    implements UseCase<ConfigurationInput, Configuration> {

  private final ConfigurationRepository configurationRepository;

  @Override
  public Configuration execute(final ConfigurationInput input) {
    final String logisticCenterId = input.getLogisticCenterId();
    final String key = input.getKey();

    configurationRepository.findById(new ConfigurationId(logisticCenterId, key))
        .ifPresent(alreadyExistsHandler(format("%s-%s", logisticCenterId, key)));

    return configurationRepository.save(Configuration.builder()
        .logisticCenterId(input.getLogisticCenterId())
        .key(input.getKey())
        .value(String.valueOf(input.getValue()))
        .metricUnit(input.getMetricUnit())
        .lastUserUpdated(input.getUserId())
        .build()
    );
  }

  private Consumer<Configuration> alreadyExistsHandler(final String configurationId) {
    return (configuration) -> {
      throw new EntityAlreadyExistsException(
          "configuration",
          configurationId);
    };
  }
}
