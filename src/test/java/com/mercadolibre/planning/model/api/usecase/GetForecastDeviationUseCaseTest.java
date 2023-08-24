package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentForecastDeviation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.GetForecastDeviationResponse;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetForecastDeviationUseCaseTest {

  @InjectMocks
  private GetForecastDeviationUseCase useCase;

  @Mock
  private CurrentForecastDeviationRepository deviationRepository;

  @Test
  void testGetForecastDeviationOk() {
    // GIVEN

    final GetForecastDeviationInput input =
        new GetForecastDeviationInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND, A_DATE_UTC);

    final CurrentForecastDeviation currentForecastDeviation = mockCurrentForecastDeviation();

    when(deviationRepository
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThan(
            input.getWarehouseId(),
            input.getWorkflow(),
            A_DATE_UTC.withFixedOffsetZone()))
        .thenReturn(List.of(currentForecastDeviation));

    // WHEN
    final List<GetForecastDeviationResponse> result = useCase.execute(input);

    // THEN
    assertFalse(result.isEmpty());

    final var output = result.get(0);
    assertEquals(DATE_IN, output.getDateFrom());
    assertEquals(DATE_OUT, output.getDateTo());
    assertEquals(0.025, output.getValue());
    assertEquals(PERCENTAGE, output.getMetricUnit());
  }

  @Test
  void testGetForecastDeviationWhenWarehouseDoesNotHave() {
    // GIVEN
    final GetForecastDeviationInput input =
        new GetForecastDeviationInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND, A_DATE_UTC);

    when(deviationRepository
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThan(
            input.getWarehouseId(),
            input.getWorkflow(),
            A_DATE_UTC.withFixedOffsetZone()))
        .thenReturn(Collections.emptyList());

    // WHEN
    final var result = useCase.execute(input);

    // THEN
    assertTrue(result.isEmpty());
  }
}
