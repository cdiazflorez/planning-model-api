package com.mercadolibre.planning.model.api.repository.current;

import com.mercadolibre.planning.model.api.dao.current.CurrentHeadcountProductivityDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProdDao;
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
        final CurrentHeadcountProductivityDao currentProdDao = mockCurrentProdDao();
        entityManager.persistAndFlush(currentProdDao);

        // WHEN
        final Optional<CurrentHeadcountProductivityDao> optCurrentProd = repository.findById(1L);

        // THEN
        assertTrue(optCurrentProd.isPresent());

        final CurrentHeadcountProductivityDao foundCurrentProd = optCurrentProd.get();
        assertEquals(currentProdDao.getId(), foundCurrentProd.getId());
        assertEquals(currentProdDao.getWorkflow(), foundCurrentProd.getWorkflow());
        assertEquals(currentProdDao.getAbilityLevel(), foundCurrentProd.getAbilityLevel());
        assertEquals(currentProdDao.getDate(), foundCurrentProd.getDate());
        assertEquals(currentProdDao.getProcessName(), foundCurrentProd.getProcessName());
        assertEquals(currentProdDao.getProductivity(), foundCurrentProd.getProductivity());
        assertEquals(currentProdDao.getProductivityMetricUnit(),
                foundCurrentProd.getProductivityMetricUnit());
        assertEquals(currentProdDao.isActive(), foundCurrentProd.isActive());
    }

    @Test
    @DisplayName("Looking for a current headcount productivity that doesn't exist, returns nothing")
    public void testCurrentHeadcountProductivityDoesntExist() {
        // WHEN
        final Optional<CurrentHeadcountProductivityDao> optCurrentHeadcountProductivity =
                repository.findById(1L);

        // THEN
        assertFalse(optCurrentHeadcountProductivity.isPresent());
    }
}
