package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.remainingprocessing.get.GetRemainingProcessingUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.web.controller.entity.EntityType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
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
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.WAVING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class GetRemainingProcessingUseCaseTest {

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @Mock
    private GetThroughputUseCase getThroughputUseCase;

    @Mock
    private GetCapacityPerHourUseCase getCapacityPerHourUseCase;

    @InjectMocks
    private GetRemainingProcessingUseCase useCase;

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

        final List<EntityOutput> throughput = getThroughputUseCaseMock();

        when(getThroughputUseCase.execute(GetEntityInput.builder()
                .workflow(FBM_WMS_OUTBOUND)
                .warehouseId(WAREHOUSE_ID)
                .entityType(EntityType.THROUGHPUT)
                .processName(List.of(PICKING, PACKING, PACKING_WALL))
                .dateFrom(A_DATE_UTC)
                .dateTo(A_DATE_UTC.plusHours(2))
                .build())).thenReturn(throughput);

        when(getCapacityPerHourUseCase.execute(any(List.class)))
                .thenReturn(List.of(
                        new CapacityOutput(A_DATE_UTC.withFixedOffsetZone(),
                                UNITS_PER_HOUR,60),
                        new CapacityOutput(A_DATE_UTC.withFixedOffsetZone().plusHours(1),
                                UNITS_PER_HOUR,40),
                        new CapacityOutput(A_DATE_UTC.withFixedOffsetZone().plusHours(2),
                                UNITS_PER_HOUR,26)
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
        assertEquals(70, remainingProcessing.getValue());
        assertEquals(A_DATE_UTC.withFixedOffsetZone(), remainingProcessing.getDate());
        assertEquals(WAVING, remainingProcessing.getProcessName());
        assertEquals(UNITS, remainingProcessing.getMetricUnit());
        assertEquals(FBM_WMS_OUTBOUND, remainingProcessing.getWorkflow());
        assertEquals(Source.FORECAST, remainingProcessing.getSource());
        assertEquals(ProcessingType.REMAINING_PROCESSING, remainingProcessing.getType());
    }

    private List<EntityOutput> getThroughputUseCaseMock() {
        return List.of(
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
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(PACKING_WALL)
                        .date(A_DATE_UTC)
                        .metricUnit(MINUTES)
                        .value(21)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(PICKING)
                        .date(A_DATE_UTC.plusHours(1))
                        .metricUnit(MINUTES)
                        .value(49)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(PACKING)
                        .date(A_DATE_UTC.plusHours(1))
                        .metricUnit(MINUTES)
                        .value(27)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(PACKING_WALL)
                        .date(A_DATE_UTC.plusHours(1))
                        .metricUnit(MINUTES)
                        .value(13)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(PICKING)
                        .date(A_DATE_UTC.plusHours(2))
                        .metricUnit(MINUTES)
                        .value(26)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(PACKING)
                        .date(A_DATE_UTC.plusHours(2))
                        .metricUnit(MINUTES)
                        .value(17)
                        .build(),
                EntityOutput.builder()
                        .workflow(FBM_WMS_OUTBOUND)
                        .processName(PACKING_WALL)
                        .date(A_DATE_UTC.plusHours(2))
                        .metricUnit(MINUTES)
                        .value(19)
                        .build()
        );
    }
}
