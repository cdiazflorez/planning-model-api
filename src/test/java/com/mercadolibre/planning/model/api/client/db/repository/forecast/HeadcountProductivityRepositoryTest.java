package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.Forecast;
import com.mercadolibre.planning.model.api.domain.entity.forecast.HeadcountProductivity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.mockHeadcountProd;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockSimpleForecast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
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
        final Optional<HeadcountProductivity> optHeadcountProd = repository.findById(productivity.getId());

        // THEN
        assertTrue(optHeadcountProd.isPresent());

        final HeadcountProductivity foundHeadcountProd = optHeadcountProd.get();
        assertEquals(productivity.getId(), foundHeadcountProd.getId());
        assertEquals(productivity.getAbilityLevel(), foundHeadcountProd.getAbilityLevel());
        assertEquals(productivity.getProcessName().name(), foundHeadcountProd.getProcessName().name());
        assertEquals(productivity.getProductivityMetricUnit().name(), foundHeadcountProd.getProductivityMetricUnit().name());
        assertEquals(productivity.getProductivity(), foundHeadcountProd.getProductivity());
        assertEquals(productivity.getDate(), foundHeadcountProd.getDate());

        final Forecast foundForecast = foundHeadcountProd.getForecast();
        assertEquals(forecast.getId(), foundForecast.getId());
        assertEquals(forecast.getWorkflow().name(), foundForecast.getWorkflow().name());
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
