package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.domain.usecase.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.GetPlanningDistributionOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
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
    @DisplayName("Get planning distribution from forecast")
    public void testGetPlanningDistributionOk() {
        // GIVEN
        final GetPlanningDistributionInput input = mockPlanningDistributionInput();

        when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutInRange(
                WAREHOUSE_ID, FBM_WMS_OUTBOUND, A_DATE_UTC, A_DATE_UTC.plusDays(3)))
                .thenReturn(planningDistributions());

        // WHEN
        final List<GetPlanningDistributionOutput> output = getPlanningDistributionUseCase
                .execute(input);

        // THEN
        final GetPlanningDistributionOutput output1 = output.get(0);
        assertEquals(A_DATE_UTC, output1.getDateIn());
        assertEquals(A_DATE_UTC.plusDays(1), output1.getDateOut());
        assertEquals(1000, output1.getTotal());
        assertEquals(UNITS, output1.getMetricUnit());

        final GetPlanningDistributionOutput output2 = output.get(1);
        assertEquals(A_DATE_UTC, output2.getDateIn());
        assertEquals(A_DATE_UTC.plusDays(2), output2.getDateOut());
        assertEquals(1200, output2.getTotal());
        assertEquals(UNITS, output2.getMetricUnit());
    }
}
