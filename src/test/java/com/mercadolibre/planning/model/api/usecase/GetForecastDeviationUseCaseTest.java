package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentForecastDeviation;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.GetForecastDeviationResponse;
import java.util.Optional;
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

    final Optional<CurrentForecastDeviation> currentForecastDeviation =
        ofNullable(mockCurrentForecastDeviation());

    when(deviationRepository
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThanEqual(
            input.getWarehouseId(),
            input.getWorkflow(),
            A_DATE_UTC.withFixedOffsetZone()))
        .thenReturn(currentForecastDeviation);

    // WHEN
    final Optional<GetForecastDeviationResponse> result = useCase.execute(input);

    // THEN
    assertTrue(result.isPresent());

    final var output = result.get();
    assertEquals(DATE_IN, output.getDateFrom());
    assertEquals(DATE_OUT, output.getDateTo());
    assertEquals(2.5, output.getValue());
    assertEquals(PERCENTAGE, output.getMetricUnit());
  }

  @Test
  void testGetForecastDeviationWhenWarehouseDoesNotHave() {
    // GIVEN
    final GetForecastDeviationInput input =
        new GetForecastDeviationInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND, A_DATE_UTC);

    when(deviationRepository
        .findByLogisticCenterIdAndWorkflowAndIsActiveTrueAndDateToIsGreaterThanEqual(
            input.getWarehouseId(),
            input.getWorkflow(),
            A_DATE_UTC.withFixedOffsetZone()))
        .thenReturn(Optional.empty());

    // WHEN
    final var result = useCase.execute(input);

    // THEN
    assertTrue(result.isEmpty());
  }
}
