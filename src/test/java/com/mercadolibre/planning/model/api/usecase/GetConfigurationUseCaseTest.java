package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.configuration.ConfigurationRepository;
import com.mercadolibre.planning.model.api.domain.entity.configuration.Configuration;
import com.mercadolibre.planning.model.api.domain.entity.configuration.ConfigurationId;
import com.mercadolibre.planning.model.api.domain.usecase.GetConfigurationUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetConfigurationInput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetConfigurationUseCaseTest {

    private static final String LOGISTIC_CENTER_ID = "ARBA01";

    private static final String KEY = "expedition_processing_time";

    @InjectMocks
    private GetConfigurationUseCase getConfiguration;

    @Mock
    private ConfigurationRepository repository;

    @Test
    public void testGetConfiguration() {
        // GIVEN
        final GetConfigurationInput input = new GetConfigurationInput(LOGISTIC_CENTER_ID, KEY);
        final Configuration configuration = new Configuration(LOGISTIC_CENTER_ID, KEY, 1, UNITS);

        when(repository.findById(new ConfigurationId(LOGISTIC_CENTER_ID, KEY)))
                .thenReturn(Optional.of(configuration));

        // WHEN
        final Optional<Configuration> configurationReturned = getConfiguration.execute(input);

        // THEN
        assertTrue(configurationReturned.isPresent());
        assertEquals(configuration, configurationReturned.get());
    }
}
