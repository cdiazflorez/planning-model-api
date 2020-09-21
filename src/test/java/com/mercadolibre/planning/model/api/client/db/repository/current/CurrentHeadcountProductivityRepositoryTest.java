package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProdEntity;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CurrentHeadcountProductivityRepositoryTest {

    @Autowired
    private CurrentHeadcountProductivityRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Looking for a current headcount productivity that exists, returns it")
    public void testFindCurrentHeadcountProductivityById() {
        // GIVEN
        final CurrentHeadcountProductivity currentProd = mockCurrentProdEntity();
        entityManager.persistAndFlush(currentProd);

        // WHEN
        final Optional<CurrentHeadcountProductivity> optCurrentProd = repository.findById(1L);

        // THEN
        assertTrue(optCurrentProd.isPresent());

        final CurrentHeadcountProductivity foundCurrentProd = optCurrentProd.get();
        assertEquals(currentProd.getId(), foundCurrentProd.getId());
        assertEquals(currentProd.getWorkflow(), foundCurrentProd.getWorkflow());
        assertEquals(currentProd.getAbilityLevel(), foundCurrentProd.getAbilityLevel());
        assertEquals(currentProd.getDate(), foundCurrentProd.getDate());
        assertEquals(currentProd.getProcessName(), foundCurrentProd.getProcessName());
        assertEquals(currentProd.getProductivity(), foundCurrentProd.getProductivity());
        assertEquals(currentProd.getProductivityMetricUnit(),
                foundCurrentProd.getProductivityMetricUnit());
        assertEquals(currentProd.isActive(), foundCurrentProd.isActive());
    }

    @Test
    @DisplayName("Looking for a current headcount productivity that doesn't exist, returns nothing")
    public void testCurrentHeadcountProductivityDoesntExist() {
        // WHEN
        final Optional<CurrentHeadcountProductivity> optProd = repository.findById(1L);

        // THEN
        assertFalse(optProd.isPresent());
    }
}