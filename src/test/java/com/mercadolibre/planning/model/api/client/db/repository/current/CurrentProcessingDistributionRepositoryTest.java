package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProcDist;
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
        final CurrentProcessingDistribution currentProcessingDist = mockCurrentProcDist();

        entityManager.persistAndFlush(currentProcessingDist);

        // WHEN
        final Optional<CurrentProcessingDistribution> optCurrentProcessingDist =
                repository.findById(1L);

        // THEN
        assertTrue(optCurrentProcessingDist.isPresent());

        final CurrentProcessingDistribution foundCurrentProc = optCurrentProcessingDist.get();
        assertEquals(currentProcessingDist.getId(), foundCurrentProc.getId());
        assertEquals(currentProcessingDist.getWorkflow(), foundCurrentProc.getWorkflow());
        assertEquals(currentProcessingDist.getType(), foundCurrentProc.getType());
        assertEquals(currentProcessingDist.getDate(), foundCurrentProc.getDate());
        assertEquals(currentProcessingDist.getProcessName(), foundCurrentProc.getProcessName());
        assertEquals(currentProcessingDist.isActive(), foundCurrentProc.isActive());
        assertEquals(currentProcessingDist.getQuantity(), foundCurrentProc.getQuantity());
        assertEquals(currentProcessingDist.getQuantityMetricUnit(),
                foundCurrentProc.getQuantityMetricUnit());
    }

    @Test
    @DisplayName("Looking for a current processing distribution that doesn't exist,"
            + " returns nothing")
    public void testCurrentProcessingDistributionDoesntExist() {
        // WHEN
        final Optional<CurrentProcessingDistribution> optDistribution = repository
                .findById(1L);

        // THEN
        assertFalse(optDistribution.isPresent());
    }
}
