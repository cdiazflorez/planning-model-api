package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetRemainingProcessingUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.output.EntityOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.REMAINING_PROCESSING;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetRemainingProcessingUseCaseTest {

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @Mock
    private GetThroughputUseCase getThroughputUseCase;

    @InjectMocks
    private GetRemainingProcessingUseCase useCase;

    @Test
    public void testGetRemainingProcessingOk() {
        // GIVEN
        final GetEntityInput input = GetEntityInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .processName(List.of(WAVING))
                .warehouseId(WAREHOUSE_ID)
                .entityType(REMAINING_PROCESSING)
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC)
                .build();

        when(getThroughputUseCase.execute(GetEntityInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .entityType(THROUGHPUT)
                .processName(List.of(PICKING, PACKING))
                .dateFrom(A_DATE_UTC.plusHours(1))
                .dateTo(A_DATE_UTC.plusHours(1))
                .build())).thenReturn(List.of(
                        EntityOutput.builder()
                                .workflow(FBM_WMS_OUTBOUND)
                                .processName(PICKING)
                                .date(A_DATE_UTC)
                                .metricUnit(MINUTES)
                                .value(60)
                                .build(),
                        EntityOutput.builder()
                                .workflow(FBM_WMS_OUTBOUND)
                                .processName(PACKING)
                                .date(A_DATE_UTC)
                                .metricUnit(MINUTES)
                                .value(45)
                                .build()
                        ));

        when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND.name(),
                Set.of(ProcessingType.REMAINING_PROCESSING.name()),
                List.of(WAVING.name()),
                A_DATE_UTC, A_DATE_UTC,
                getForecastWeeks(A_DATE_UTC, A_DATE_UTC))
        ).thenReturn(List.of(new ProcessingDistributionViewImpl(
                1,
                Date.from(A_DATE_UTC.toInstant()),
                WAVING,
                100,
                MINUTES,
                ProcessingType.REMAINING_PROCESSING)));

        // WHEN
        final List<EntityOutput> outputs = useCase.execute(input);

        // THEN
        assertNotNull(outputs);
        assertEquals(1, outputs.size());

        final EntityOutput remainingProcessing = outputs.get(0);

        assertNotNull(remainingProcessing);
        assertEquals(75, remainingProcessing.getValue());
        assertEquals(A_DATE_UTC.withFixedOffsetZone(), remainingProcessing.getDate());
        assertEquals(WAVING, remainingProcessing.getProcessName());
        assertEquals(UNITS, remainingProcessing.getMetricUnit());
    }
}
