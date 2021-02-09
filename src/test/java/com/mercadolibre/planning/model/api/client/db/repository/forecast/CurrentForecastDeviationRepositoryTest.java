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
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentForecastDeviation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CurrentForecastDeviationRepositoryTest {

    @Autowired
    private CurrentForecastDeviationRepository repository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Find currectForecastDeviation active by warehouse id and workflow")
    public void findBylogisticCenterIdAndWorkflowAndIsActiveOk() {
        // GIVEN
        final CurrentForecastDeviation currentForecastDeviation =
                mockCurrentForecastDeviation();
        entityManager.persist(currentForecastDeviation);
        entityManager.flush();

        // WHEN
        final CurrentForecastDeviation deviation =
                repository.findBylogisticCenterIdAndWorkflowAndIsActive(
                        WAREHOUSE_ID, FBM_WMS_OUTBOUND, true).get();

        // THEN
        assertNotNull(deviation);
        assertEquals(WAREHOUSE_ID, deviation.getLogisticCenterId());
        assertEquals(DATE_IN, deviation.getDateFrom());
        assertEquals(DATE_OUT, deviation.getDateTo());
        assertEquals(0.025, deviation.getValue());
        assertEquals("FBM_WMS_OUTBOUND", deviation.getWorkflow().name());
    }

    @Test
    @DisplayName("Find currectForecastDeviation when not exist deviation for a warehouseId")
    public void findBylogisticCenterIdAndWorkflowAndIsActiveWhenNotExistDeviation() {
        // GIVEN

        // WHEN
        final Optional<CurrentForecastDeviation>  deviation =
                repository.findBylogisticCenterIdAndWorkflowAndIsActive(
                        WAREHOUSE_ID, FBM_WMS_OUTBOUND, true);

        // THEN
        assertNotNull(deviation);
        assertFalse(deviation.isPresent());
    }

}
