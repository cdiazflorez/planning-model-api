package com.mercadolibre.planning.model.api.repository.forecast;

import com.mercadolibre.planning.model.api.dao.forecast.ForecastDao;
import com.mercadolibre.planning.model.api.dao.forecast.ForecastMetadataDao;
import com.mercadolibre.planning.model.api.dao.forecast.ForecastMetadataDaoId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.FORECAST_METADATA_KEY;
import static com.mercadolibre.planning.model.api.util.TestUtils.FORECAST_METADATA_VALUE;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastMetadataDao;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecastDao;
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
        final ForecastDao forecastDao = mockSimpleForecastDao();
        entityManager.persistAndFlush(forecastDao);

        final ForecastMetadataDao forecastMetadataDao = mockForecastMetadataDao(forecastDao);

        entityManager.persistAndFlush(forecastMetadataDao);

        // WHEN
        final Optional<ForecastMetadataDao> optForecastMetadataDao = repository
                .findById(mockForecastMetadataDaoId());

        // THEN
        assertTrue(optForecastMetadataDao.isPresent());

        final ForecastMetadataDao foundForecastMetadataDao = optForecastMetadataDao.get();
        assertEquals(1L, foundForecastMetadataDao.getForecastId());
        assertEquals(FORECAST_METADATA_KEY, foundForecastMetadataDao.getKey());
        assertEquals(FORECAST_METADATA_VALUE, foundForecastMetadataDao.getValue());
    }

    @Test
    @DisplayName("Looking for a forecast metadata that doesn't exist, returns nothing")
    public void testForecastMetadataDoesntExist() {
        // WHEN
        final Optional<ForecastMetadataDao> optForecastMetadataDao = repository
                .findById(mockForecastMetadataDaoId());

        // THEN
        assertFalse(optForecastMetadataDao.isPresent());
    }

    private ForecastMetadataDaoId mockForecastMetadataDaoId() {
        return new ForecastMetadataDaoId(1L, FORECAST_METADATA_KEY);
    }
}
