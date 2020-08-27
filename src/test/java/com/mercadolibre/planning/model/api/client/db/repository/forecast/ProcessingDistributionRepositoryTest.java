package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.ProcessingDistributionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProcessingDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ProcessingDistributionRepositoryTest {

    @Autowired
    private ProcessingDistributionRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Looking for a processing distribution that exists, returns it")
    public void testFindProcessingDistributionById() {
        // GIVEN
        final ForecastEntity forecastEntity = mockSimpleForecast();
        entityManager.persistAndFlush(forecastEntity);
        entityManager.persistAndFlush(mockProcessingDist(forecastEntity));

        // WHEN
        final Optional<ProcessingDistributionEntity> optProcessingDistribution =
                repository.findById(1L);

        // THEN
        assertTrue(optProcessingDistribution.isPresent());

        final ProcessingDistributionEntity foundProcessingDistribution =
                optProcessingDistribution.get();

        assertEquals(1L, foundProcessingDistribution.getId());
        assertEquals(A_DATE, foundProcessingDistribution.getDate());
        assertEquals(1000, foundProcessingDistribution.getQuantity());
        assertEquals("UNIT", foundProcessingDistribution.getQuantityMetricUnit().name());
        assertEquals("WAVING", foundProcessingDistribution.getProcessName().name());
        assertEquals("REMAINING_PROCESSING", foundProcessingDistribution.getType().name());

        final ForecastEntity foundForecastEntity = foundProcessingDistribution.getForecast();
        assertEquals(1L, foundForecastEntity.getId());
        assertEquals("FBM_WMS_OUTBOUND", foundForecastEntity.getWorkflow().name());
    }

    @Test
    @DisplayName("Looking for a processing distribution that doesn't exist, returns nothing")
    public void testProcessingDistributionDoesntExist() {
        // WHEN
        final Optional<ProcessingDistributionEntity> optProcessingDist = repository.findById(1L);

        // THEN
        assertFalse(optProcessingDist.isPresent());
    }
}
