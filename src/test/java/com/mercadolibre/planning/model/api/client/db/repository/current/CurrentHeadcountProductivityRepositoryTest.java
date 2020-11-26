package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.current.CurrentHeadcountProductivity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;
import java.util.Optional;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.USER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProdEntity;
import static java.util.Collections.singletonList;
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
        final CurrentHeadcountProductivity currentProd = mockCurrentProdEntity(A_DATE_UTC, 68L);
        entityManager.persistAndFlush(currentProd);

        // WHEN
        final Optional<CurrentHeadcountProductivity> optCurrentProd = repository.findById(1L);

        // THEN
        assertTrue(optCurrentProd.isPresent());

        final CurrentHeadcountProductivity foundCurrentProd = optCurrentProd.get();
        assertCurrentProductivity(currentProd, foundCurrentProd);
    }

    @Test
    @DisplayName("Looking for a current headcount productivity that doesn't exist, returns nothing")
    public void testCurrentHeadcountProductivityDoesntExist() {
        // WHEN
        final Optional<CurrentHeadcountProductivity> optProd = repository.findById(1L);

        // THEN
        assertFalse(optProd.isPresent());
    }

    @Test
    @DisplayName("Looking for a current headcount productivity that exists by simulation "
            + "filtered with params and returns it")
    public void testFindCurrentHeadcountProductivityByFilters() {
        // GIVEN
        final CurrentHeadcountProductivity currentProd = mockCurrentProdEntity(A_DATE_UTC, 68L);
        entityManager.persistAndFlush(currentProd);

        // WHEN
        final List<CurrentHeadcountProductivity> optCurrentProd = repository
                .findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                        currentProd.getLogisticCenterId(),
                        currentProd.getWorkflow(),
                        List.of(PICKING, PACKING),
                        currentProd.getDate().minusDays(1),
                        currentProd.getDate().plusDays(1)
                );

        // THEN
        assertFalse(optCurrentProd.isEmpty());

        final CurrentHeadcountProductivity foundCurrentProd = optCurrentProd.get(0);
        assertCurrentProductivity(currentProd, foundCurrentProd);
    }

    @Test
    @DisplayName("Deactivate productivity")
    public void testDeactivateProductivity() {
        // GIVEN
        final CurrentHeadcountProductivity currentProd = mockCurrentProdEntity(A_DATE_UTC, 68L);
        entityManager.persistAndFlush(currentProd);

        // WHEN
        repository.deactivateProductivity(WAREHOUSE_ID, FBM_WMS_OUTBOUND, PICKING,
                singletonList(A_DATE_UTC), UNITS_PER_HOUR, USER_ID, 1L);

        final Optional<CurrentHeadcountProductivity> result = repository.findById(1L);

        // THEN
        assertTrue(result.isPresent());
        assertFalse(result.get().isActive());
        assertEquals(USER_ID, result.get().getUserId());
    }

    private void assertCurrentProductivity(final CurrentHeadcountProductivity currentProd,
                                           final CurrentHeadcountProductivity foundCurrentProd) {
        assertEquals(currentProd.getId(), foundCurrentProd.getId());
        assertEquals(currentProd.getWorkflow(), foundCurrentProd.getWorkflow());
        assertEquals(currentProd.getAbilityLevel(), foundCurrentProd.getAbilityLevel());
        assertEquals(currentProd.getLogisticCenterId(), foundCurrentProd.getLogisticCenterId());
        assertEquals(currentProd.getDate(), foundCurrentProd.getDate());
        assertEquals(currentProd.getProcessName(), foundCurrentProd.getProcessName());
        assertEquals(currentProd.getProductivity(), foundCurrentProd.getProductivity());
        assertEquals(currentProd.getProductivityMetricUnit(),
                foundCurrentProd.getProductivityMetricUnit());
        assertEquals(currentProd.isActive(), foundCurrentProd.isActive());
    }
}
