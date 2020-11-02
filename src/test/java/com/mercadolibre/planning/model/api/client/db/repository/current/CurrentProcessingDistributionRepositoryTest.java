package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Optional;

import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProcDist;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class CurrentProcessingDistributionRepositoryTest {

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
        whenCurrentDistributionIsOk(currentProcessingDist, foundCurrentProc);
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

    @Test
    @DisplayName("Looking for a current processing distributions that exists filterin by "
            + "different params, returns it")
    public void testFindCurrentProcessingDistributionBySimulations() {
        // GIVEN
        final CurrentProcessingDistribution currentProcessingDist = mockCurrentProcDist();

        entityManager.persistAndFlush(currentProcessingDist);

        // WHEN
        final List<CurrentProcessingDistribution> currentProcessingDistList =
                repository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        currentProcessingDist.getLogisticCenterId(),
                        currentProcessingDist.getWorkflow(), currentProcessingDist.getType(),
                        List.of(ProcessName.PICKING, ProcessName.PACKING),
                        currentProcessingDist.getDate().minusHours(1),
                        currentProcessingDist.getDate().plusHours(1));

        // THEN
        assertEquals(1, currentProcessingDistList.size());

        final CurrentProcessingDistribution foundCurrentProc = currentProcessingDistList.get(0);
        whenCurrentDistributionIsOk(currentProcessingDist, foundCurrentProc);
    }

    private void whenCurrentDistributionIsOk(final CurrentProcessingDistribution currentProcessing,
                                             final CurrentProcessingDistribution foundCurrentProc) {
        assertEquals(currentProcessing.getId(), foundCurrentProc.getId());
        assertEquals(currentProcessing.getWorkflow(), foundCurrentProc.getWorkflow());
        assertEquals(currentProcessing.getType(), foundCurrentProc.getType());
        assertEquals(currentProcessing.getDate(), foundCurrentProc.getDate());
        assertEquals(currentProcessing.getProcessName(), foundCurrentProc.getProcessName());
        assertEquals(currentProcessing.isActive(), foundCurrentProc.isActive());
        assertEquals(currentProcessing.getQuantity(), foundCurrentProc.getQuantity());
        assertEquals(currentProcessing.getQuantityMetricUnit(),
                foundCurrentProc.getQuantityMetricUnit());
        assertEquals(currentProcessing.getLogisticCenterId(),
                foundCurrentProc.getLogisticCenterId());
    }
}
