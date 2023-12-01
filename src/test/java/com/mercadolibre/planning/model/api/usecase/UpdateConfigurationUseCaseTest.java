package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.util.TestUtils.CONFIG_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.configuration.ConfigurationId;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.ConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.update.UpdateConfigurationUseCase;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateConfigurationUseCaseTest {

  @InjectMocks
  private UpdateConfigurationUseCase updateConfiguration;

  @Mock
  private ConfigurationRepository repository;

  public static Stream<Arguments> testArguments() {
    return Stream.of(
        Arguments.of(0),
        Arguments.of(123)
    );
  }

  @ParameterizedTest
  @MethodSource("testArguments")
  void testUpdateConfiguration(final long userId) {
    // GIVEN
    final ConfigurationInput input = new ConfigurationInput(
        LOGISTIC_CENTER_ID, CONFIG_KEY, 60, MINUTES, userId);

    final Configuration updatedConfiguration = Configuration.builder()
        .logisticCenterId(LOGISTIC_CENTER_ID)
        .key(CONFIG_KEY)
        .value("60")
        .metricUnit(MINUTES)
        .lastUserUpdated(userId)
        .build();

    final Configuration configuration = Configuration.builder()
        .logisticCenterId(LOGISTIC_CENTER_ID)
        .key(CONFIG_KEY)
        .value("1")
        .metricUnit(UNITS)
        .build();

    when(repository.findById(new ConfigurationId(LOGISTIC_CENTER_ID, CONFIG_KEY)))
        .thenReturn(Optional.of(configuration));

    when(repository.save(updatedConfiguration)).thenReturn(updatedConfiguration);

    // WHEN
    final Configuration configurationResult = updateConfiguration.execute(input);

    // THEN
    assertEquals(updatedConfiguration, configurationResult);
  }
}
