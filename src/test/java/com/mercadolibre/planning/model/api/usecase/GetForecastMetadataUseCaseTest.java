package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataView;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.GetForecastMetadataUseCase;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetForecastMetadataUseCaseTest {

    @Mock
    private ForecastMetadataRepository forecastMetadataRepository;

    @InjectMocks
    private GetForecastMetadataUseCase forecastHistoricSis;

    @Test
    @DisplayName("Get Forecast Metadata Info")
    public void testGetInfo() {
        // GIVEN
        final GetForecastMetadataInput input  = mockForecastMetadataInput();
        when(forecastMetadataRepository.findLastForecastMetadataByWarehouseId(
                WAREHOUSE_ID)
        ).thenReturn(mockForecastByWarehouseId());

        // WHEN
        final List<ForecastMetadataView> forecastMetadata = forecastHistoricSis.execute(input);

        //THEN
        assertEquals(5, forecastMetadata.size());
        forecastMetadataEqualTo(forecastMetadata.get(0),
                "mono_order_distribution", "58");
        forecastMetadataEqualTo(forecastMetadata.get(1),
                "multi_order_distribution", "23");
        forecastMetadataEqualTo(forecastMetadata.get(2),
                "multi_batch_distribution", "72");
        forecastMetadataEqualTo(forecastMetadata.get(3),
                "warehouse_id", "ARBA01");
        forecastMetadataEqualTo(forecastMetadata.get(4),
                "week", "48-2020");

    }

    @Test
    @DisplayName("Get Forecast Metadata NO Info")
    public void testNoInfo() {

        // GIVEN
        final GetForecastMetadataInput input  = mockForecastMetadataInput();
        when(forecastMetadataRepository.findLastForecastMetadataByWarehouseId(
                WAREHOUSE_ID)
        ).thenReturn(List.of());

        // WHEN
        final List<ForecastMetadataView> forecastMetadata = forecastHistoricSis.execute(input);

        //THEN
        assertTrue(forecastMetadata.isEmpty());
    }

    private void forecastMetadataEqualTo(final ForecastMetadataView output,
                                         final String key,
                                         final String value) {

        assertEquals(key, output.getKey());
        assertEquals(value, output.getValue());
    }
}
