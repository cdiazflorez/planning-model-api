package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.util.TestUtils.CONFIG_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.configuration.ConfigurationId;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.ConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.CreateConfigurationUseCase;
import com.mercadolibre.planning.model.api.exception.EntityAlreadyExistsException;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateConfigurationUseCaseTest {

  @InjectMocks
  private CreateConfigurationUseCase createConfiguration;

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
  void testCreateConfiguration(final long userId) {
    // GIVEN
    final ConfigurationInput input = new ConfigurationInput(
        LOGISTIC_CENTER_ID, CONFIG_KEY, 60, MINUTES, userId);

    final Configuration configuration = Configuration.builder()
        .logisticCenterId(LOGISTIC_CENTER_ID)
        .key(CONFIG_KEY)
        .value("60")
        .metricUnit(MINUTES)
        .lastUserUpdated(userId)
        .build();

    when(repository.findById(new ConfigurationId(LOGISTIC_CENTER_ID, CONFIG_KEY)))
        .thenReturn(Optional.empty());

    when(repository.save(configuration)).thenReturn(configuration);

    // WHEN
    final Configuration configurationResult = createConfiguration.execute(input);

    // THEN
    assertEquals(configuration, configurationResult);
  }

  @Test
  void testCreateConfigurationAlreadyExists() {
    // GIVEN
    final ConfigurationInput input = new ConfigurationInput(
        LOGISTIC_CENTER_ID, CONFIG_KEY, 60, MINUTES, 0);

    final String configId = format("%s-%s", LOGISTIC_CENTER_ID, CONFIG_KEY);
    when(repository.findById(new ConfigurationId(LOGISTIC_CENTER_ID, CONFIG_KEY)))
        .thenThrow(new EntityAlreadyExistsException("configuration", configId));

    // WHEN - THEN
    assertThrows(EntityAlreadyExistsException.class, () -> createConfiguration.execute(input));
  }
}

