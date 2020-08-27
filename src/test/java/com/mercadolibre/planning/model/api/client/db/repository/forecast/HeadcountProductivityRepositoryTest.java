package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.ForecastEntity;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivityEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.AN_OFFSET_TIME;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountProd;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
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
        final ForecastEntity forecastEntity = mockSimpleForecast();
        entityManager.persistAndFlush(forecastEntity);

        final HeadcountProductivityEntity productivity = mockHeadcountProd(forecastEntity);
        entityManager.persistAndFlush(productivity);

        // WHEN
        final Optional<HeadcountProductivityEntity> optHeadcountProd = repository.findById(1L);

        // THEN
        assertTrue(optHeadcountProd.isPresent());

        final HeadcountProductivityEntity foundHeadcountProd = optHeadcountProd.get();
        assertEquals(1L, foundHeadcountProd.getId());
        assertEquals(1L, foundHeadcountProd.getAbilityLevel());
        assertEquals("PACKING", foundHeadcountProd.getProcessName().name());
        assertEquals("PERCENTAGE", foundHeadcountProd.getProductivityMetricUnit().name());
        assertEquals(80L, foundHeadcountProd.getProductivity());
        assertEquals(AN_OFFSET_TIME, foundHeadcountProd.getDayTime());

        final ForecastEntity foundForecastEntity = foundHeadcountProd.getForecast();
        assertEquals(1L, foundForecastEntity.getId());
        assertEquals("FBM_WMS_OUTBOUND", foundForecastEntity.getWorkflow().name());
    }

    @Test
    @DisplayName("Looking for a headcount productivity that doesn't exist, returns nothing")
    public void testHeadcountProductivityDoesntExist() {
        // WHEN
        final Optional<HeadcountProductivityEntity> optHeadcountProd = repository.findById(1L);

        // THEN
        assertFalse(optHeadcountProd.isPresent());
    }
}
