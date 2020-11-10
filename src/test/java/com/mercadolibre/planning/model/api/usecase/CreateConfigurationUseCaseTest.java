package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.configuration.ConfigurationId;
import com.mercadolibre.planning.model.api.domain.usecase.CreateConfigurationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.ConfigurationInput;
import com.mercadolibre.planning.model.api.exception.EntityAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.util.TestUtils.CONFIG_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateConfigurationUseCaseTest {

    @InjectMocks
    private CreateConfigurationUseCase createConfiguration;

    @Mock
    private ConfigurationRepository repository;

    @Test
    public void testCreateConfiguration() {
        // GIVEN
        final ConfigurationInput input = new ConfigurationInput(
                LOGISTIC_CENTER_ID, CONFIG_KEY, 60, MINUTES);

        final Configuration configuration = Configuration.builder()
                .logisticCenterId(LOGISTIC_CENTER_ID)
                .key(CONFIG_KEY)
                .value(60)
                .metricUnit(MINUTES)
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
    public void testCreateConfigurationAlreadyExists() {
        // GIVEN
        final ConfigurationInput input = new ConfigurationInput(
                LOGISTIC_CENTER_ID, CONFIG_KEY, 60, MINUTES);

        final String configId = format("%s-%s", LOGISTIC_CENTER_ID, CONFIG_KEY);
        when(repository.findById(new ConfigurationId(LOGISTIC_CENTER_ID, CONFIG_KEY)))
                .thenThrow(new EntityAlreadyExistsException("configuration", configId));

        // WHEN - THEN
        assertThrows(EntityAlreadyExistsException.class, () -> createConfiguration.execute(input));
    }
}

