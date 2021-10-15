package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.search.SearchEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SearchEntityUseCaseTest {

    @InjectMocks
    private SearchEntityUseCase useCase;

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @Mock
    private GetForecastUseCase getForecastUseCase;

    @Test
    public void testGetPerformedProcessingOk() {
        // GIVEN
        final GetEntityInput input = GetEntityInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .processName(List.of(WAVING))
                .warehouseId(WAREHOUSE_ID)
                .entityType(EntityType.PERFORMED_PROCESSING)
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC)
                .build();

        final List<Long> forecastIds = List.of(1L);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())).thenReturn(forecastIds);

        when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                Set.of(ProcessingType.PERFORMED_PROCESSING.name()),
                List.of(WAVING.name()),
                A_DATE_UTC, A_DATE_UTC,
                forecastIds)
        ).thenReturn(List.of(new ProcessingDistributionViewImpl(
                1,
                Date.from(A_DATE_UTC.toInstant()),
                WAVING,
                308,
                MINUTES,
                ProcessingType.PERFORMED_PROCESSING)));

        // WHEN
        final List<EntityOutput> outputs = useCase.execute(input);

        // THEN
        assertNotNull(outputs);
        assertEquals(1, outputs.size());

        final EntityOutput performedProcessing = outputs.get(0);

        assertNotNull(performedProcessing);
        assertEquals(308, performedProcessing.getValue());
        assertEquals(A_DATE_UTC.withFixedOffsetZone(), performedProcessing.getDate());
        assertEquals(WAVING, performedProcessing.getProcessName());
        assertEquals(MINUTES, performedProcessing.getMetricUnit());
        assertEquals(FBM_WMS_OUTBOUND, performedProcessing.getWorkflow());
        assertEquals(FORECAST, performedProcessing.getSource());
        assertEquals(ProcessingType.PERFORMED_PROCESSING, performedProcessing.getType());
    }

    @Test
    public void testGetRemainingProcessingOk() {
        // GIVEN
        final GetEntityInput input = GetEntityInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .processName(List.of(WAVING))
                .warehouseId(WAREHOUSE_ID)
                .entityType(EntityType.REMAINING_PROCESSING)
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC)
                .build();

        final List<Long> forecastIds = List.of(1L);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())).thenReturn(forecastIds);

        when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                Set.of(ProcessingType.REMAINING_PROCESSING.name()),
                List.of(WAVING.name()),
                A_DATE_UTC, A_DATE_UTC,
                forecastIds)
        ).thenReturn(List.of(new ProcessingDistributionViewImpl(
                1,
                Date.from(A_DATE_UTC.toInstant()),
                WAVING,
                308,
                MINUTES,
                ProcessingType.REMAINING_PROCESSING)));

        // WHEN
        final List<EntityOutput> outputs = useCase.execute(input);

        // THEN
        assertNotNull(outputs);
        assertEquals(1, outputs.size());

        final EntityOutput remainingProcessing = outputs.get(0);

        assertNotNull(remainingProcessing);
        assertEquals(308, remainingProcessing.getValue());
        assertEquals(A_DATE_UTC.withFixedOffsetZone(), remainingProcessing.getDate());
        assertEquals(WAVING, remainingProcessing.getProcessName());
        assertEquals(MINUTES, remainingProcessing.getMetricUnit());
        assertEquals(FBM_WMS_OUTBOUND, remainingProcessing.getWorkflow());
        assertEquals(FORECAST, remainingProcessing.getSource());
        assertEquals(ProcessingType.REMAINING_PROCESSING, remainingProcessing.getType());
    }
}
