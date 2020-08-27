package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadataEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadataEntityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.FORECAST_METADATA_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.FORECAST_METADATA_VALUE;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastMetadata;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ForecastMetadataRepositoryTest {

    @Autowired
    private ForecastMetadataRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Looking for a forecast metadata that exists, returns it")
    public void testFindForecastMetadataById() {
        // GIVEN
        final ForecastEntity forecastEntity = mockSimpleForecast();
        entityManager.persistAndFlush(forecastEntity);

        final ForecastMetadataEntity forecastMetadataEntity = mockForecastMetadata(forecastEntity);

        entityManager.persistAndFlush(forecastMetadataEntity);

        // WHEN
        final Optional<ForecastMetadataEntity> optForecastMetadata = repository
                .findById(mockForecastMetadataId());

        // THEN
        assertTrue(optForecastMetadata.isPresent());

        final ForecastMetadataEntity foundForecastMetadata = optForecastMetadata.get();
        assertEquals(1L, foundForecastMetadata.getForecastId());
        assertEquals(FORECAST_METADATA_KEY, foundForecastMetadata.getKey());
        assertEquals(FORECAST_METADATA_VALUE, foundForecastMetadata.getValue());
    }

    @Test
    @DisplayName("Looking for a forecast metadata that doesn't exist, returns nothing")
    public void testForecastMetadataDoesntExist() {
        // WHEN
        final Optional<ForecastMetadataEntity> optForecastMetadata = repository
                .findById(mockForecastMetadataId());

        // THEN
        assertFalse(optForecastMetadata.isPresent());
    }

    private ForecastMetadataEntityId mockForecastMetadataId() {
        return new ForecastMetadataEntityId(1L, FORECAST_METADATA_KEY);
    }
}
