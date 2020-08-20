package com.mercadolibre.planning.model.api.repository.forecast;

import com.mercadolibre.planning.model.api.dao.forecast.ForecastDao;
import com.mercadolibre.planning.model.api.dao.forecast.HeadcountProductivityDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.AN_OFFSET_TIME;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountProdDao;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecastDao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class HeadcountProductivityRepositoryTest {

    @Autowired
    private HeadcountProductivityRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Looking for a headcount productivity that exists, returns it")
    public void testFindHeadcountProductivityById() {
        // GIVEN
        final ForecastDao forecastDao = mockSimpleForecastDao();
        entityManager.persistAndFlush(forecastDao);

        final HeadcountProductivityDao productivityDao = mockHeadcountProdDao(forecastDao);
        entityManager.persistAndFlush(productivityDao);

        // WHEN
        final Optional<HeadcountProductivityDao> optHeadcountProductivity = repository.findById(1L);

        // THEN
        assertTrue(optHeadcountProductivity.isPresent());

        final HeadcountProductivityDao foundHeadcountProductivity = optHeadcountProductivity.get();
        assertEquals(1L, foundHeadcountProductivity.getId());
        assertEquals(1L, foundHeadcountProductivity.getAbilityLevel());
        assertEquals("PACKING", foundHeadcountProductivity.getProcessName().name());
        assertEquals("PERCENTAGE", foundHeadcountProductivity.getProductivityMetricUnit().name());
        assertEquals(80L, foundHeadcountProductivity.getProductivity());
        assertEquals(AN_OFFSET_TIME, foundHeadcountProductivity.getDayTime());

        final ForecastDao foundForecastDao = foundHeadcountProductivity.getForecast();
        assertEquals(1L, foundForecastDao.getId());
        assertEquals("FBM_WMS_OUTBOUND", foundForecastDao.getWorkflow().name());
    }

    @Test
    @DisplayName("Looking for a headcount productivity that doesn't exist, returns nothing")
    public void testHeadcountProductivityDaoDoesntExist() {
        // WHEN
        final Optional<HeadcountProductivityDao> optHeadcountProductivity = repository.findById(1L);

        // THEN
        assertFalse(optHeadcountProductivity.isPresent());
    }
}
