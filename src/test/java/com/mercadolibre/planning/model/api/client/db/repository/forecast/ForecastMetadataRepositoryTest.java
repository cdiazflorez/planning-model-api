package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadataId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
        final Forecast forecast = entityManager.persistAndFlush(mockSimpleForecast());

       final ForecastMetadata forecastMetadata = entityManager.persistAndFlush(mockForecastMetadata(forecast));

       final ForecastMetadataId forecastMetadataId = new ForecastMetadataId(forecastMetadata.getForecastId(), forecastMetadata.getKey());

        // WHEN
        final Optional<ForecastMetadata> optForecastMetadata = repository.findById(forecastMetadataId);

        // THEN
        assertTrue(optForecastMetadata.isPresent());

        final ForecastMetadata foundForecastMetadata = optForecastMetadata.get();
        assertEquals(forecast.getId(), foundForecastMetadata.getForecastId());
        assertEquals(FORECAST_METADATA_KEY, foundForecastMetadata.getKey());
        assertEquals(FORECAST_METADATA_VALUE, foundForecastMetadata.getValue());
    }

    @Test
    @DisplayName("Looking for a forecast metadata that doesn't exist, returns nothing")
    public void testForecastMetadataDoesntExist() {
        // WHEN
        final Optional<ForecastMetadata> optForecastMetadata = repository
                .findById(mockForecastMetadataId());

        // THEN
        assertFalse(optForecastMetadata.isPresent());
    }

    private ForecastMetadataId mockForecastMetadataId() {
        return new ForecastMetadataId(1L, FORECAST_METADATA_KEY);
    }
}
