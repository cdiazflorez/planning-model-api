package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
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
        final Forecast forecast = mockSimpleForecast();
        entityManager.persistAndFlush(forecast);

        final HeadcountProductivity productivity = mockHeadcountProd(forecast);
        entityManager.persistAndFlush(productivity);

        // WHEN
        final Optional<HeadcountProductivity> optHeadcountProd = repository.findById(1L);

        // THEN
        assertTrue(optHeadcountProd.isPresent());

        final HeadcountProductivity foundHeadcountProd = optHeadcountProd.get();
        assertEquals(1L, foundHeadcountProd.getId());
        assertEquals(1L, foundHeadcountProd.getAbilityLevel());
        assertEquals("PACKING", foundHeadcountProd.getProcessName().name());
        assertEquals("PERCENTAGE", foundHeadcountProd.getProductivityMetricUnit().name());
        assertEquals(80L, foundHeadcountProd.getProductivity());
        assertEquals(AN_OFFSET_TIME, foundHeadcountProd.getDayTime());

        final Forecast foundForecast = foundHeadcountProd.getForecast();
        assertEquals(1L, foundForecast.getId());
        assertEquals("FBM_WMS_OUTBOUND", foundForecast.getWorkflow().name());
    }

    @Test
    @DisplayName("Looking for a headcount productivity that doesn't exist, returns nothing")
    public void testHeadcountProductivityDoesntExist() {
        // WHEN
        final Optional<HeadcountProductivity> optHeadcountProd = repository.findById(1L);

        // THEN
        assertFalse(optHeadcountProd.isPresent());
    }
}
