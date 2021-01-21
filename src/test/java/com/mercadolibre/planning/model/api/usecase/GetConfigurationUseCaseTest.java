package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.configuration.ConfigurationId;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationInput;
import com.mercadolibre.planning.model.api.domain.usecase.configuration.get.GetConfigurationUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.util.TestUtils.CONFIG_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetConfigurationUseCaseTest {

    @InjectMocks
    private GetConfigurationUseCase getConfiguration;

    @Mock
    private ConfigurationRepository repository;

    @Test
    public void testGetConfiguration() {
        // GIVEN
        final GetConfigurationInput input = new GetConfigurationInput(
                LOGISTIC_CENTER_ID, CONFIG_KEY);

        final Configuration configuration = Configuration.builder()
                .logisticCenterId(LOGISTIC_CENTER_ID)
                .key(CONFIG_KEY)
                .value(1)
                .metricUnit(UNITS)
                .build();

        when(repository.findById(new ConfigurationId(LOGISTIC_CENTER_ID, CONFIG_KEY)))
                .thenReturn(Optional.of(configuration));

        // WHEN
        final Optional<Configuration> configurationReturned = getConfiguration.execute(input);

        // THEN
        assertTrue(configurationReturned.isPresent());
        assertEquals(configuration, configurationReturned.get());
    }
}
