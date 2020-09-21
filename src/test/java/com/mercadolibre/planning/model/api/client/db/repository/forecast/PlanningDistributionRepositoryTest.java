package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.PlanningDistribution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.HashSet;
import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PlanningDistributionRepositoryTest {

    @Autowired
    private PlanningDistributionRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Looking for a planning distribution that exists, returns it")
    public void testFindPlanningDistributionById() {
        // GIVEN
        final Forecast forecast = mockSimpleForecast();
        entityManager.persistAndFlush(forecast);
        entityManager.persistAndFlush(mockPlanningDist(forecast));

        // WHEN
        final Optional<PlanningDistribution> optPlanningDist = repository.findById(1L);

        // THEN
        assertTrue(optPlanningDist.isPresent());

        final PlanningDistribution foundPlanningDistribution = optPlanningDist.get();
        assertEquals(1L, foundPlanningDistribution.getId());
        assertEquals(DATE_IN, foundPlanningDistribution.getDateIn());
        assertEquals(DATE_OUT, foundPlanningDistribution.getDateOut());
        assertEquals(1200, foundPlanningDistribution.getQuantity());
        assertEquals("UNIT", foundPlanningDistribution.getQuantityMetricUnit().name());
        assertEquals(new HashSet<>(), foundPlanningDistribution.getMetadatas());

        final Forecast foundForecast = foundPlanningDistribution.getForecast();
        assertEquals(1L, foundForecast.getId());
        assertEquals("FBM_WMS_OUTBOUND", foundForecast.getWorkflow().name());
    }

    @Test
    @DisplayName("Looking for a planning distribution that doesn't exist, returns nothing")
    public void testPlanningDistributionDoesntExist() {
        // WHEN
        final Optional<PlanningDistribution> optPlanningDist = repository.findById(1L);

        // THEN
        assertFalse(optPlanningDist.isPresent());
    }
}
