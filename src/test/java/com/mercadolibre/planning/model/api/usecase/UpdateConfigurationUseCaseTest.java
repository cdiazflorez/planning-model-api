package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.configuration.ConfigurationId;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.create.ConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.update.UpdateConfigurationUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.util.TestUtils.CONFIG_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateConfigurationUseCaseTest {

    @InjectMocks
    private UpdateConfigurationUseCase updateConfiguration;

    @Mock
    private ConfigurationRepository repository;

    @Test
    public void testUpdateConfiguration() {
        // GIVEN
        final ConfigurationInput input = new ConfigurationInput(
                LOGISTIC_CENTER_ID, CONFIG_KEY, 60, MINUTES);

        final Configuration updatedConfiguration = Configuration.builder()
                .logisticCenterId(LOGISTIC_CENTER_ID)
                .key(CONFIG_KEY)
                .value("60")
                .metricUnit(MINUTES)
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
