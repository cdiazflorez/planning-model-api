package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastRepository;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastIdView;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetForecastUseCaseTest {
    @Mock
    private ForecastRepository forecastRepository;

    @InjectMocks
    private GetForecastUseCase getForecastUseCase;

    @Test
    @DisplayName("Get Forecast by warehouse, workflow and weeks OK")
    public void testGetForecastOK() {
        // GIVEN
        final GetForecastInput input = GetForecastInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateTo(A_DATE_UTC)
                .dateFrom(A_DATE_UTC.plusDays(1))
                .build();

        when(forecastRepository.findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND.name(),
                getForecastWeeks(input.getDateFrom(), input.getDateTo())
        )).thenReturn(mockForecastIdView());

        // WHEN
        final List<Long> forecasts = getForecastUseCase.execute(input);

        // THEN
        assertFalse(forecasts.isEmpty());
        assertEquals(2, forecasts.size());

        final Long forecastId1 = forecasts.get(0);
        assertEquals(Long.valueOf(1), forecastId1);

        final Long forecastId2 = forecasts.get(1);
        assertEquals(Long.valueOf(2), forecastId2);
    }

    @Test
    @DisplayName("When no forecast is present an exception must be thrown")
    public void testThrowExceptionWhenForecastNotFound() {
        // GIVEN
        final GetForecastInput input = GetForecastInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .dateTo(A_DATE_UTC)
                .dateFrom(A_DATE_UTC.plusDays(1))
                .build();

        when(forecastRepository.findLastForecastIdByWarehouseIdAAndWorkflowAndWeeks(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND.name(),
                getForecastWeeks(input.getDateFrom(), input.getDateTo())
        )).thenReturn(List.of());

        // WHEN - THEN
        assertThrows(ForecastNotFoundException.class, () -> getForecastUseCase.execute(input));
    }

}
