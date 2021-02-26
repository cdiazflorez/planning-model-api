package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.deviation.get.GetForecastDeviationUseCase;
import com.mercadolibre.planning.model.api.exception.EntityNotFoundException;
import com.mercadolibre.planning.model.api.web.controller.deviation.response.GetForecastDeviationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.PERCENTAGE;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_IN;
import static com.mercadolibre.planning.model.api.util.TestUtils.DATE_OUT;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentForecastDeviation;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetForecastDeviationUseCaseTest {

    @InjectMocks
    private GetForecastDeviationUseCase useCase;

    @Mock
    private CurrentForecastDeviationRepository deviationRepository;

    @Test
    public void testGetForecastDeviationOk() {
        // GIVEN

        final GetForecastDeviationInput input =
                new GetForecastDeviationInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND, A_DATE_UTC);

        final Optional<CurrentForecastDeviation> currentForecastDeviation =
                ofNullable(mockCurrentForecastDeviation());

        when(deviationRepository.findByLogisticCenterIdAndWorkflowAndIsActiveAndDateInRange(
                input.getWarehouseId(),
                input.getWorkflow().name(),
                true,
                A_DATE_UTC.withFixedOffsetZone()))
                .thenReturn(currentForecastDeviation);

        // WHEN
        final GetForecastDeviationResponse output = useCase.execute(input);

        // THEN

        assertNotNull(output);
        assertEquals(DATE_IN, output.getDateFrom());
        assertEquals(DATE_OUT, output.getDateTo());
        assertEquals(2.5, output.getValue());
        assertEquals(PERCENTAGE, output.getMetricUnit());
    }

    @Test
    public void testGetForecastDeviationWhenWarehouseDoesNotHave() {
        // GIVEN

        final GetForecastDeviationInput input =
                new GetForecastDeviationInput(WAREHOUSE_ID, FBM_WMS_OUTBOUND, A_DATE_UTC);

        when(deviationRepository.findByLogisticCenterIdAndWorkflowAndIsActiveAndDateInRange(
                input.getWarehouseId(),
                input.getWorkflow().name(),
                true,
                A_DATE_UTC.withFixedOffsetZone()))
                .thenReturn(Optional.empty());

        // THEN
        assertThrows(EntityNotFoundException.class, () -> useCase.execute(input));
    }
}
