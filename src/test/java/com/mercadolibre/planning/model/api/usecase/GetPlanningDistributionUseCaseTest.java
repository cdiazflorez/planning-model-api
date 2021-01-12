package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDistributionInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.planningDistributions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetPlanningDistributionUseCaseTest {

    @Mock
    private PlanningDistributionRepository planningDistRepository;

    @InjectMocks
    private GetPlanningDistributionUseCase getPlanningDistributionUseCase;

    @Test
    @DisplayName("Get planning distribution from forecast without date in to")
    public void testGetPlanningDistributionOk() {
        // GIVEN
        final GetPlanningDistributionInput input = mockPlanningDistributionInput(null);

        when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutInRange(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND.name(),
                A_DATE_UTC,
                A_DATE_UTC.plusDays(3),
                getForecastWeeks(A_DATE_UTC, A_DATE_UTC.plusDays(3)))
        ).thenReturn(planningDistributions());

        // WHEN
        final List<GetPlanningDistributionOutput> output = getPlanningDistributionUseCase
                .execute(input);

        // THEN
        final GetPlanningDistributionOutput output1 = output.get(0);
        assertEquals(A_DATE_UTC.toInstant(), output1.getDateIn().toInstant());
        assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
        assertEquals(1000, output1.getTotal());
        assertEquals(UNITS, output1.getMetricUnit());

        final GetPlanningDistributionOutput output2 = output.get(1);
        assertEquals(A_DATE_UTC.toInstant(), output2.getDateIn().toInstant());
        assertEquals(A_DATE_UTC.plusDays(2).toInstant(), output2.getDateOut().toInstant());
        assertEquals(1200, output2.getTotal());
        assertEquals(UNITS, output2.getMetricUnit());
    }

    @Test
    @DisplayName("Get planning distribution from forecast wih date in to")
    public void testGetPlanningDistributionWithDateInToOk() {
        // GIVEN
        final ZonedDateTime dateInTo = A_DATE_UTC.minusDays(3);
        final GetPlanningDistributionInput input = mockPlanningDistributionInput(dateInTo);
        when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutInRangeAndDateInLessThan(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND.name(),
                A_DATE_UTC,
                A_DATE_UTC.plusDays(3),
                dateInTo,
                getForecastWeeks(A_DATE_UTC, A_DATE_UTC.plusDays(3)))
        ).thenReturn(planningDistributions());

        // WHEN
        final List<GetPlanningDistributionOutput> output = getPlanningDistributionUseCase
                .execute(input);

        // THEN
        final GetPlanningDistributionOutput output1 = output.get(0);
        assertEquals(A_DATE_UTC.toInstant(), output1.getDateIn().toInstant());
        assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
        assertEquals(1000, output1.getTotal());
        assertEquals(UNITS, output1.getMetricUnit());

        final GetPlanningDistributionOutput output2 = output.get(1);
        assertEquals(A_DATE_UTC.toInstant(), output2.getDateIn().toInstant());
        assertEquals(A_DATE_UTC.plusDays(2).toInstant(), output2.getDateOut().toInstant());
        assertEquals(1200, output2.getTotal());
        assertEquals(UNITS, output2.getMetricUnit());
    }
}
