package com.mercadolibre.planning.model.api.repository.forecast;

import com.mercadolibre.planning.model.api.dao.forecast.ForecastDao;
import com.mercadolibre.planning.model.api.dao.forecast.ForecastMetadataDao;
import com.mercadolibre.planning.model.api.dao.forecast.HeadcountDistributionDao;
import com.mercadolibre.planning.model.api.dao.forecast.HeadcountProductivityDao;
import com.mercadolibre.planning.model.api.dao.forecast.PlanningDistributionDao;
import com.mercadolibre.planning.model.api.dao.forecast.PlanningDistributionMetadataDao;
import com.mercadolibre.planning.model.api.dao.forecast.ProcessingDistributionDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;
import java.util.Set;

import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastDao;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastMetadataDao;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountDistDao;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountProdDao;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDistribution;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockProcessingDistDao;
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
        final ForecastDao forecastDao = persistForecastDao();

        // WHEN
        final Optional<ForecastDao> optForecastDao = repository.findById(forecastDao.getId());

        // THEN
        assertTrue(optForecastDao.isPresent());

        final ForecastDao foundForecastDao = optForecastDao.get();
        assertEquals(forecastDao.getId(), foundForecastDao.getId());
        assertEquals(forecastDao.getWorkflow(), foundForecastDao.getWorkflow());
        assertEquals(forecastDao.getDateCreated(), foundForecastDao.getDateCreated());
        assertEquals(forecastDao.getLastUpdated(), foundForecastDao.getLastUpdated());
        assertEquals(forecastDao.getMetadatas(), foundForecastDao.getMetadatas());

        assertEquals(forecastDao.getPlanningDistributions(),
                foundForecastDao.getPlanningDistributions());

        assertEquals(forecastDao.getHeadcountDistributions(),
                foundForecastDao.getHeadcountDistributions());

        assertEquals(forecastDao.getProcessingDistributions(),
                foundForecastDao.getProcessingDistributions());

        assertEquals(forecastDao.getHeadcountProductivities(),
                foundForecastDao.getHeadcountProductivities());
    }

    @Test
    @DisplayName("Looking for a forecast that doesn't exist, returns nothing")
    public void testForecastDoesntExist() {
        // WHEN
        final Optional<ForecastDao> optForecastDao = repository.findById(1L);

        // THEN
        assertFalse(optForecastDao.isPresent());
    }

    private ForecastDao persistForecastDao() {
        final HeadcountDistributionDao headcountDistributionDao = mockHeadcountDistDao(null);
        entityManager.persistAndFlush(headcountDistributionDao);

        final HeadcountProductivityDao headcountProductivityDao = mockHeadcountProdDao(null);
        entityManager.persistAndFlush(headcountProductivityDao);

        final PlanningDistributionDao planningDistributionDao = mockPlanningDistribution(null);
        entityManager.persistAndFlush(planningDistributionDao);

        final ProcessingDistributionDao processingDistributionDao = mockProcessingDistDao(null);
        entityManager.persistAndFlush(processingDistributionDao);

        final ForecastDao forecastDao = mockForecastDao(
                Set.of(headcountDistributionDao),
                Set.of(headcountProductivityDao),
                Set.of(planningDistributionDao),
                Set.of(processingDistributionDao),
                null);

        entityManager.persistAndFlush(forecastDao);

        final ForecastMetadataDao forecastMetadataDao = mockForecastMetadataDao(forecastDao);
        entityManager.persistAndFlush(forecastMetadataDao);

        return forecastDao;
    }
}
