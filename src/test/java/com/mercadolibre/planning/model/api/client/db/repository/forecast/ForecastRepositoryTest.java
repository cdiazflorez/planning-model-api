package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastMetadataEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistributionEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivityEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistributionEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistributionEntity;
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
        final ForecastEntity forecastEntity = persistForecast();

        // WHEN
        final Optional<ForecastEntity> optForecast = repository.findById(forecastEntity.getId());

        // THEN
        assertTrue(optForecast.isPresent());

        final ForecastEntity foundForecastEntity = optForecast.get();
        assertEquals(forecastEntity.getId(), foundForecastEntity.getId());
        assertEquals(forecastEntity.getWorkflow(), foundForecastEntity.getWorkflow());
        assertEquals(forecastEntity.getDateCreated(), foundForecastEntity.getDateCreated());
        assertEquals(forecastEntity.getLastUpdated(), foundForecastEntity.getLastUpdated());
        assertEquals(forecastEntity.getMetadatas(), foundForecastEntity.getMetadatas());

        assertEquals(forecastEntity.getPlanningDistributions(),
                foundForecastEntity.getPlanningDistributions());

        assertEquals(forecastEntity.getHeadcountDistributions(),
                foundForecastEntity.getHeadcountDistributions());

        assertEquals(forecastEntity.getProcessingDistributions(),
                foundForecastEntity.getProcessingDistributions());

        assertEquals(forecastEntity.getHeadcountProductivities(),
                foundForecastEntity.getHeadcountProductivities());
    }

    @Test
    @DisplayName("Looking for a forecast that doesn't exist, returns nothing")
    public void testForecastDoesntExist() {
        // WHEN
        final Optional<ForecastEntity> optForecast = repository.findById(1L);

        // THEN
        assertFalse(optForecast.isPresent());
    }

    private ForecastEntity persistForecast() {
        final HeadcountDistributionEntity headcountDistributionEntity = mockHeadcountDist(null);
        entityManager.persistAndFlush(headcountDistributionEntity);

        final HeadcountProductivityEntity headcountProductivityEntity = mockHeadcountProd(null);
        entityManager.persistAndFlush(headcountProductivityEntity);

        final PlanningDistributionEntity planningDistributionEntity = mockPlanningDist(null);
        entityManager.persistAndFlush(planningDistributionEntity);

        final ProcessingDistributionEntity processingDistributionEntity = mockProcessingDist(null);
        entityManager.persistAndFlush(processingDistributionEntity);

        final ForecastEntity forecastEntity = mockForecast(
                Set.of(headcountDistributionEntity),
                Set.of(headcountProductivityEntity),
                Set.of(planningDistributionEntity),
                Set.of(processingDistributionEntity),
                null);

        entityManager.persistAndFlush(forecastEntity);

        final ForecastMetadataEntity forecastMetadataEntity = mockForecastMetadata(forecastEntity);
        entityManager.persistAndFlush(forecastMetadataEntity);

        return forecastEntity;
    }
}
