package com.mercadolibre.planning.model.api.domain.usecase.plan.staffing;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.EFFECTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE;
import static com.mercadolibre.planning.model.api.util.TestUtils.LOGISTIC_CENTER_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.PROCESS_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.plan.CurrentStaffingPlanInput;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlan;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlanInput;
import com.mercadolibre.planning.model.api.domain.entity.plan.StaffingPlanResponse;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.gateway.CurrentProcessingDistributionGateway;
import com.mercadolibre.planning.model.api.gateway.ProcessingDistributionGateway;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetStaffingPlanUseCaseTest {

  private static final String DATE_STRING = "2024-01-01T00:00:00Z";

  private static final String PICKING_PROCESS = "picking";

  private static final String PACKING_PROCESS = "picking";

  private static final Map<String, String> GROUP_ONE = Map.of(
      PROCESS_NAME, PICKING_PROCESS,
      DATE, DATE_STRING
  );

  private static final Map<String, String> GROUP_TWO = Map.of(
      PROCESS_NAME, PACKING_PROCESS,
      DATE, DATE_STRING
  );

  @InjectMocks
  private GetStaffingPlanUseCase getStaffingPlanUseCase;

  @Mock
  private GetForecastUseCase getForecastUseCase;

  @Mock
  private ProcessingDistributionGateway processingDistributionGateway;

  @Mock
  private CurrentProcessingDistributionGateway currentProcessingDistributionGateway;

  @Test
  void testGetStaffingPlan() {
    //GIVEN
    final Instant date = Instant.parse(DATE_STRING);

    final Set<StaffingPlanResponse> expectedResponse = Set.of(
        new StaffingPlanResponse(
            10,
            5,
            GROUP_ONE
        ),
        new StaffingPlanResponse(
            10,
            10,
            GROUP_TWO
        )
    );

    when(getForecastUseCase.execute(any(GetForecastInput.class))).thenReturn(List.of(1L, 2L));
    when(processingDistributionGateway.getStaffingPlan(any(StaffingPlanInput.class))).thenReturn(mockProcessingDistribution());
    when(currentProcessingDistributionGateway.getCurrentStaffingPlan(any(CurrentStaffingPlanInput.class)))
        .thenReturn(mockCurrentProcessingDistribution());

    //WHEN
    final var result = getStaffingPlanUseCase.getStaffingPlan(
        LOGISTIC_CENTER_ID,
        Workflow.FBM_WMS_OUTBOUND,
        EFFECTIVE_WORKERS,
        List.of(PROCESS_NAME, DATE),
        Map.of(PROCESS_NAME, List.of(PICKING_PROCESS, PACKING_PROCESS)),
        date,
        date,
        date
    );

    //THEN
    assertEquals(expectedResponse, new HashSet<>(result));
  }

  private static List<StaffingPlan> mockProcessingDistribution() {
    return List.of(
        new StaffingPlan(
            5,
            WORKERS,
            EFFECTIVE_WORKERS,
            GROUP_ONE
        ),
        new StaffingPlan(
            10,
            WORKERS,
            EFFECTIVE_WORKERS,
            GROUP_TWO
        )
    );
  }

  private static List<StaffingPlan> mockCurrentProcessingDistribution() {
    return List.of(
        new StaffingPlan(
            10,
            WORKERS,
            EFFECTIVE_WORKERS,
            GROUP_ONE
        )
    );
  }

}
