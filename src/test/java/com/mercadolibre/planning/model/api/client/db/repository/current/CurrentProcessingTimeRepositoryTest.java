package com.mercadolibre.planning.model.api.client.db.repository.current;

import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
public class CurrentProcessingTimeRepositoryTest {

    @Autowired
    private CurrentProcessingTimeRepository repository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    public void testFindCurrentProcessingTimeByDate() {
        // GIVEN
        final CurrentProcessingTime currentProcessingTime =
                mockEntity(A_DATE_UTC, A_DATE_UTC.plusHours(2));

        testEntityManager.persistAndFlush(currentProcessingTime);

        // WHEN
        final List<CurrentProcessingTime> queryResult = repository
                .findByWorkflowAndLogisticCenterIdAndIsActiveTrueAndDateBetweenCpt(
                        FBM_WMS_OUTBOUND,
                        WAREHOUSE_ID,
                        A_DATE_UTC.plusHours(1)
                );

        // THEN
        assertFalse(queryResult.isEmpty());
        assertEquals(1, queryResult.size());
        assertEquals(currentProcessingTime.getId(), queryResult.get(0).getId());
    }

    @Test
    public void testFindCurrentProcessingTimeDoesntExist() {
        // GIVEN
        final CurrentProcessingTime currentProcessingTime =
                mockEntity(A_DATE_UTC, A_DATE_UTC.plusHours(2));

        testEntityManager.persistAndFlush(currentProcessingTime);

        // WHEN
        final List<CurrentProcessingTime> result = repository
                .findByWorkflowAndLogisticCenterIdAndIsActiveTrueAndDateBetweenCpt(
                        FBM_WMS_OUTBOUND,
                        WAREHOUSE_ID,
                        A_DATE_UTC.minusHours(1)
                );

        // THEN
        assertTrue(result.isEmpty());
    }

    private CurrentProcessingTime mockEntity(
            final ZonedDateTime cptFrom, final ZonedDateTime cptTo) {
        return CurrentProcessingTime.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .logisticCenterId(WAREHOUSE_ID)
                .isActive(true)
                .cptFrom(cptFrom)
                .cptTo(cptTo)
                .value(300)
                .metricUnit(MINUTES)
                .build();
    }
}
