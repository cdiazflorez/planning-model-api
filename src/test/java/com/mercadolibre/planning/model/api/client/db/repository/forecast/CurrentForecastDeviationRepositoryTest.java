package com.mercadolibre.planning.model.api.client.db.repository.forecast;

import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentForecastDeviation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CurrentForecastDeviationRepositoryTest {

    @Autowired
    private CurrentForecastDeviationRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Find currentForecastDeviation active by warehouse id and workflow")
    public void findByLogisticCenterIdAndWorkflowAndIsActiveAndDateInRangeOk() {
        // GIVEN
        final CurrentForecastDeviation currentForecastDeviation =
                mockCurrentForecastDeviation();
        entityManager.persist(currentForecastDeviation);
        entityManager.flush();

        // WHEN
        final Optional<CurrentForecastDeviation> optDeviation =
                repository.findByLogisticCenterIdAndWorkflowAndIsActiveAndDateInRange(
                        WAREHOUSE_ID, FBM_WMS_OUTBOUND.name(), true, DATE_IN.plusHours(1));

        // THEN
        assertTrue(optDeviation.isPresent());

        final CurrentForecastDeviation deviation = optDeviation.get();
        assertEquals(WAREHOUSE_ID, deviation.getLogisticCenterId());
        assertEquals(DATE_IN, deviation.getDateFrom());
        assertEquals(DATE_OUT, deviation.getDateTo());
        assertEquals(0.025, deviation.getValue());
        assertEquals("FBM_WMS_OUTBOUND", deviation.getWorkflow().name());
    }

    @Test
    @DisplayName("Find currentForecastDeviation when no deviation exists for a warehouseId")
    public void findByLogisticCenterIdAndWorkflowAndIsActiveWhenNotExistDeviation() {
        // WHEN
        final Optional<CurrentForecastDeviation> optDeviation =
                repository.findByLogisticCenterIdAndWorkflowAndIsActiveAndDateInRange(
                        WAREHOUSE_ID, FBM_WMS_OUTBOUND.toJson(), true, A_DATE_UTC);

        // THEN
        assertFalse(optDeviation.isPresent());
    }
}
