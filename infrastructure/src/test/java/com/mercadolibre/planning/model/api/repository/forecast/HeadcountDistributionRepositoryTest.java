package com.mercadolibre.planning.model.api.repository.forecast;

import com.mercadolibre.planning.model.api.dao.forecast.ForecastDao;
import com.mercadolibre.planning.model.api.dao.forecast.HeadcountDistributionDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountDistDao;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecastDao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class HeadcountDistributionRepositoryTest {

    @Autowired
    private HeadcountDistributionRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Looking for a headcount distribution that exists, returns it")
    public void testFindHeadcountDistributionById() {
        // GIVEN
        final ForecastDao forecastDao = mockSimpleForecastDao();
        entityManager.persistAndFlush(forecastDao);

        final HeadcountDistributionDao distributionDao = mockHeadcountDistDao(forecastDao);
        entityManager.persistAndFlush(distributionDao);

        // WHEN
        final Optional<HeadcountDistributionDao> optHeadcountDistribution = repository.findById(1L);

        // THEN
        assertTrue(optHeadcountDistribution.isPresent());

        final HeadcountDistributionDao foundHeadcountDistribution = optHeadcountDistribution.get();
        assertEquals(1L, foundHeadcountDistribution.getId());
        assertEquals("MZ", foundHeadcountDistribution.getArea());
        assertEquals("PICKING", foundHeadcountDistribution.getProcessName().name());
        assertEquals(40, foundHeadcountDistribution.getQuantity());
        assertEquals("WORKER", foundHeadcountDistribution.getQuantityMetricUnit().name());

        final ForecastDao foundForecastDao = foundHeadcountDistribution.getForecast();
        assertEquals(1L, foundForecastDao.getId());
        assertEquals("FBM_WMS_OUTBOUND", foundForecastDao.getWorkflow().name());
    }

    @Test
    @DisplayName("Looking for a headcount distribution that doesn't exist, returns nothing")
    public void testHeadcountDistributionDoesntExist() {
        // WHEN
        final Optional<HeadcountDistributionDao> optHeadcountDistributionDao =
                repository.findById(1L);

        // THEN
        assertFalse(optHeadcountDistributionDao.isPresent());
    }
}
