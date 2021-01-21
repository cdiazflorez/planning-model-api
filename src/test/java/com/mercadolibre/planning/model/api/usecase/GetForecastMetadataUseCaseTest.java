package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ForecastMetadataView;
import com.mercadolibre.planning.model.api.domain.entity.WaveCardinality;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastMetadataUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
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
        final Set<String> forecastWeeks = getForecastWeeks(input.getDateFrom(), input.getDateTo());
        when(forecastMetadataRepository.findLastForecastMetadataByWarehouseId(
                List.of(
                        WaveCardinality.MONO_ORDER_DISTRIBUTION.toJson(),
                        WaveCardinality.MULTI_BATCH_DISTRIBUTION.toJson(),
                        WaveCardinality.MULTI_ORDER_DISTRIBUTION.toJson()
                ),
                WAREHOUSE_ID,
                input.getWorkflow().name(),
                forecastWeeks)
        ).thenReturn(mockForecastByWarehouseId());

        // WHEN
        final List<ForecastMetadataView> forecastMetadata = forecastHistoricSis.execute(input);

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
        final Set<String> forecastWeeks = getForecastWeeks(input.getDateFrom(), input.getDateTo());
        when(forecastMetadataRepository.findLastForecastMetadataByWarehouseId(
                List.of(
                        WaveCardinality.MONO_ORDER_DISTRIBUTION.toJson(),
                        WaveCardinality.MULTI_BATCH_DISTRIBUTION.toJson(),
                        WaveCardinality.MULTI_ORDER_DISTRIBUTION.toJson()
                ),
                WAREHOUSE_ID,
                input.getWorkflow().name(),
                forecastWeeks)
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
