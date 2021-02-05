package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataView;
import com.mercadolibre.planning.model.api.domain.entity.WaveCardinality;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.exception.ForecastNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastByWarehouseId;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastMetadataInput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetForecastMetadataUseCaseTest {

    @Mock
    private ForecastMetadataRepository forecastMetadataRepository;

    @Mock
    private GetForecastUseCase getForecastUseCase;

    @InjectMocks
    private GetForecastMetadataUseCase getForecastMetadataUseCase;

    @Test
    @DisplayName("Get Forecast Metadata Info")
    public void testGetInfo() {
        // GIVEN
        final GetForecastMetadataInput input  = mockForecastMetadataInput();
        final List<Long> lastForecasts = List.of(1L, 2L);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())
        ).thenReturn(lastForecasts);

        when(forecastMetadataRepository.findLastForecastMetadataByWarehouseId(
                List.of(
                        WaveCardinality.MONO_ORDER_DISTRIBUTION.toJson(),
                        WaveCardinality.MULTI_BATCH_DISTRIBUTION.toJson(),
                        WaveCardinality.MULTI_ORDER_DISTRIBUTION.toJson()
                ),
                lastForecasts)
        ).thenReturn(mockForecastByWarehouseId());

        // WHEN
        final List<ForecastMetadataView> forecastMetadata =
                getForecastMetadataUseCase.execute(input);

        //THEN
        assertEquals(3, forecastMetadata.size());
        forecastMetadataEqualTo(forecastMetadata.get(0),
                "mono_order_distribution", "20");
        forecastMetadataEqualTo(forecastMetadata.get(1),
                "multi_order_distribution", "20");
        forecastMetadataEqualTo(forecastMetadata.get(2),
                "multi_batch_distribution", "60");
    }

    @Test
    @DisplayName("Get Forecast Metadata NO Info")
    public void testNoInfo() {
        // GIVEN
        final GetForecastMetadataInput input  = mockForecastMetadataInput();
        final List<Long> lastForecasts = List.of(1L, 2L);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(WAREHOUSE_ID)
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())
        ).thenReturn(lastForecasts);

        when(forecastMetadataRepository.findLastForecastMetadataByWarehouseId(
                List.of(
                        WaveCardinality.MONO_ORDER_DISTRIBUTION.toJson(),
                        WaveCardinality.MULTI_BATCH_DISTRIBUTION.toJson(),
                        WaveCardinality.MULTI_ORDER_DISTRIBUTION.toJson()
                ),
                lastForecasts)
        ).thenReturn(List.of());

        // WHEN
        final List<ForecastMetadataView> forecastMetadata =
                getForecastMetadataUseCase.execute(input);

        //THEN
        assertTrue(forecastMetadata.isEmpty());
    }

    @Test
    @DisplayName("Forecast ID not found for given warehouse, workflow and weeks")
    public void testNoForecastIdPresent() {
        // GIVEN
        final GetForecastMetadataInput input  = mockForecastMetadataInput();

        doThrow(ForecastNotFoundException.class).when(getForecastUseCase)
                .execute(GetForecastInput.builder()
                        .workflow(input.getWorkflow())
                        .warehouseId(WAREHOUSE_ID)
                        .dateFrom(input.getDateFrom())
                        .dateTo(input.getDateTo())
                        .build());

        // WHEN
        assertThrows(ForecastNotFoundException.class, () ->
                getForecastMetadataUseCase.execute(input));

        //THEN
        verifyZeroInteractions(forecastMetadataRepository);
    }

    private void forecastMetadataEqualTo(final ForecastMetadataView output,
                                         final String key,
                                         final String value) {

        assertEquals(key, output.getKey());
        assertEquals(value, output.getValue());
    }
}
