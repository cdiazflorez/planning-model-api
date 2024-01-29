package com.mercadolibre.planning.model.api.usecase.simulation.update;

import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static org.mockito.Mockito.verify;

import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanUseCase.CreateSimulationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.update.UpdateStaffingPlanUseCase.CurrentProcessingDistributionGateway;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UpdateStaffingPlanUseCaseTest {
  private static final String LOGISTIC_CENTER_ID = "ARTW01";
  private static final String PROCESS_NAME = "PICKING";
  private static final String PROCESS_NAME_KEY = "process";
  @Mock
  private CurrentProcessingDistributionGateway currentProcessingDistributionGateway;

  private UpdateStaffingPlanUseCase updateStaffingPlanUseCase;

  @BeforeEach
  void setUp() {
    updateStaffingPlanUseCase = new UpdateStaffingPlanUseCase(currentProcessingDistributionGateway);
  }

  @Test
  void shouldUpdateStaffingPlan() {

    //Given
    final UpdateStaffingPlanInput input = createUpdateStaffingPlanDto();
    final List<CreateSimulationInput> results = List.of(
        createSimulationInput(1.0, DATE_IN, EntityType.HEADCOUNT),
        createSimulationInput(2.0, DATE_IN.plusHours(1), EntityType.HEADCOUNT),
        createSimulationInput(3.0, DATE_IN.plusHours(2), EntityType.HEADCOUNT),
        createSimulationInput(4.0, DATE_IN, EntityType.PRODUCTIVITY),
        createSimulationInput(5.0, DATE_IN.plusHours(1), EntityType.PRODUCTIVITY),
        createSimulationInput(6.0, DATE_IN.plusHours(2), EntityType.PRODUCTIVITY)
    );

    //When
    updateStaffingPlanUseCase.execute(input);

    //Then
    verify(currentProcessingDistributionGateway).createStaffingUpdates(results);

  }

  private UpdateStaffingPlanInput createUpdateStaffingPlanDto() {
    return new UpdateStaffingPlanInput(
        LOGISTIC_CENTER_ID,
        Workflow.FBM_WMS_OUTBOUND,
        1L,
        List.of(
            new UpdateStaffingPlanInput.Resource(
                EntityType.HEADCOUNT,
                List.of(
                    new UpdateStaffingPlanInput.ResourceValues(
                        1.0,
                        DATE_IN,
                        Map.of(PROCESS_NAME_KEY, PROCESS_NAME)
                    ),
                    new UpdateStaffingPlanInput.ResourceValues(
                        2.0,
                        DATE_IN.plusHours(1),
                        Map.of(PROCESS_NAME_KEY, PROCESS_NAME)
                    ),
                    new UpdateStaffingPlanInput.ResourceValues(
                        3.0,
                        DATE_IN.plusHours(2),
                        Map.of(PROCESS_NAME_KEY, PROCESS_NAME)
                    )
                )
            ),
            new UpdateStaffingPlanInput.Resource(
                EntityType.PRODUCTIVITY,
                List.of(
                    new UpdateStaffingPlanInput.ResourceValues(
                        4.0,
                        DATE_IN,
                        Map.of(PROCESS_NAME_KEY, PROCESS_NAME)
                    ),
                    new UpdateStaffingPlanInput.ResourceValues(
                        5.0,
                        DATE_IN.plusHours(1),
                        Map.of(PROCESS_NAME_KEY, PROCESS_NAME)
                    ),
                    new UpdateStaffingPlanInput.ResourceValues(
                        6.0,
                        DATE_IN.plusHours(2),
                        Map.of(PROCESS_NAME_KEY, PROCESS_NAME)
                    )
                )
            )
        )
    );
  }

  private CreateSimulationInput createSimulationInput(
      final double quantity,
      final ZonedDateTime date,
      final EntityType entityType
  ) {
    return new CreateSimulationInput(
        Workflow.FBM_WMS_OUTBOUND,
        LOGISTIC_CENTER_ID,
        1L,
        Map.of(PROCESS_NAME_KEY, PROCESS_NAME),
        quantity,
        date,
        entityType
    );
  }
}
