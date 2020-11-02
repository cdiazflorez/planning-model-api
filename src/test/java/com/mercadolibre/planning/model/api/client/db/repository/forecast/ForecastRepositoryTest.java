package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadata;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistribution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;
import java.util.Set;

import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecast;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastMetadata;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountProd;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProcessingDist;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ForecastRepositoryTest {

    @Autowired
    private ForecastRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Looking for a forecast that exists, returns it")
    public void testFindForecastById() {
        // GIVEN
        final Forecast forecast = persistForecast();

        // WHEN
        final Optional<Forecast> optForecast = repository.findById(forecast.getId());

        // THEN
        assertTrue(optForecast.isPresent());

        final Forecast foundForecast = optForecast.get();
        assertEquals(forecast.getId(), foundForecast.getId());
        assertEquals(forecast.getWorkflow(), foundForecast.getWorkflow());
        assertEquals(forecast.getDateCreated(), foundForecast.getDateCreated());
        assertEquals(forecast.getLastUpdated(), foundForecast.getLastUpdated());
        assertEquals(forecast.getMetadatas(), foundForecast.getMetadatas());

        assertEquals(forecast.getPlanningDistributions(),
                foundForecast.getPlanningDistributions());

        assertEquals(forecast.getHeadcountDistributions(),
                foundForecast.getHeadcountDistributions());

        assertEquals(forecast.getProcessingDistributions(),
                foundForecast.getProcessingDistributions());

        assertEquals(forecast.getHeadcountProductivities(),
                foundForecast.getHeadcountProductivities());
    }

    @Test
    @DisplayName("Looking for a forecast that doesn't exist, returns nothing")
    public void testForecastDoesntExist() {
        // WHEN
        final Optional<Forecast> optForecast = repository.findById(1L);

        // THEN
        assertFalse(optForecast.isPresent());
    }

    private Forecast persistForecast() {
        final HeadcountDistribution headcountDistribution = mockHeadcountDist(null);
        entityManager.persistAndFlush(headcountDistribution);

        final HeadcountProductivity headcountProductivity = mockHeadcountProd(null);
        entityManager.persistAndFlush(headcountProductivity);

        final PlanningDistribution planningDistribution = mockPlanningDist(null);
        entityManager.persistAndFlush(planningDistribution);

        final ProcessingDistribution processingDistribution = mockProcessingDist(null);
        entityManager.persistAndFlush(processingDistribution);

        final Forecast forecast = mockForecast(
                Set.of(headcountDistribution),
                Set.of(headcountProductivity),
                Set.of(planningDistribution),
                Set.of(processingDistribution),
                null);

        entityManager.persistAndFlush(forecast);

        final ForecastMetadata forecastMetadata = mockForecastMetadata(forecast);
        entityManager.persistAndFlush(forecastMetadata);

        return forecast;
    }
}
