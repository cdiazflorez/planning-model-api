package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountDistribution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
        final Forecast forecast = mockSimpleForecast();
        entityManager.persistAndFlush(forecast);

        final HeadcountDistribution headcountDistribution = mockHeadcountDist(forecast);
        entityManager.persistAndFlush(headcountDistribution);

        // WHEN
        final Optional<HeadcountDistribution> optHeadcountDist = repository.findById(headcountDistribution.getId());

        // THEN
        assertTrue(optHeadcountDist.isPresent());

        final HeadcountDistribution foundHeadcountDistribution = optHeadcountDist.get();
        assertEquals(headcountDistribution.getId(), foundHeadcountDistribution.getId());
        assertEquals(headcountDistribution.getArea(), foundHeadcountDistribution.getArea());
        assertEquals(headcountDistribution.getProcessName().name(), foundHeadcountDistribution.getProcessName().name());
        assertEquals(headcountDistribution.getQuantity(), foundHeadcountDistribution.getQuantity());
        assertEquals(headcountDistribution.getQuantityMetricUnit().name(), foundHeadcountDistribution.getQuantityMetricUnit().name());

        final Forecast foundForecast = foundHeadcountDistribution.getForecast();
        assertEquals(forecast.getId(), foundForecast.getId());
        assertEquals(forecast.getWorkflow().name(), foundForecast.getWorkflow().name());
    }

    @Test
    @DisplayName("Looking for a headcount distribution that doesn't exist, returns nothing")
    public void testHeadcountDistributionDoesntExist() {
        // WHEN
        final Optional<HeadcountDistribution> optHeadcountDistribution =
                repository.findById(1L);

        // THEN
        assertFalse(optHeadcountDistribution.isPresent());
    }
}
