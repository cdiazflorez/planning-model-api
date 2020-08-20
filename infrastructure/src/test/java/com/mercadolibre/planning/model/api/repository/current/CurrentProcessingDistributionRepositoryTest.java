package com.mercadolibre.planning.model.api.repository.current;

import com.mercadolibre.planning.model.api.dao.current.CurrentProcessingDistributionDao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProcDistDao;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CurrentProcessingDistributionRepositoryTest {

    @Autowired
    private CurrentProcessingDistributionRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Looking for a current processing distribution that exists, returns it")
    public void testFindCurrentProcessingDistributionById() {
        // GIVEN
        final CurrentProcessingDistributionDao currentProcessingDistDao = mockCurrentProcDistDao();
        entityManager.persistAndFlush(currentProcessingDistDao);

        // WHEN
        final Optional<CurrentProcessingDistributionDao> optCurrentProcessingDist =
                repository.findById(1L);

        // THEN
        assertTrue(optCurrentProcessingDist.isPresent());

        final CurrentProcessingDistributionDao foundCurrentProc = optCurrentProcessingDist.get();
        assertEquals(currentProcessingDistDao.getId(), foundCurrentProc.getId());
        assertEquals(currentProcessingDistDao.getWorkflow(), foundCurrentProc.getWorkflow());
        assertEquals(currentProcessingDistDao.getType(), foundCurrentProc.getType());
        assertEquals(currentProcessingDistDao.getDate(), foundCurrentProc.getDate());
        assertEquals(currentProcessingDistDao.getProcessName(), foundCurrentProc.getProcessName());
        assertEquals(currentProcessingDistDao.isActive(), foundCurrentProc.isActive());
        assertEquals(currentProcessingDistDao.getQuantity(), foundCurrentProc.getQuantity());
        assertEquals(currentProcessingDistDao.getQuantityMetricUnit(),
                foundCurrentProc.getQuantityMetricUnit());
    }

    @Test
    @DisplayName("Looking for a current processing distribution that doesn't exist,"
            + " returns nothing")
    public void testCurrentProcessingDistributionDoesntExist() {
        // WHEN
        final Optional<CurrentProcessingDistributionDao> optCurrentProcessingDistributionDao =
                repository.findById(1L);

        // THEN
        assertFalse(optCurrentProcessingDistributionDao.isPresent());
    }
}
