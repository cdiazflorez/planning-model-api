package com.mercadolibre.planning.model.api.usecase.entities;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.ProcessingType;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.headcount.get.GetHeadcountInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.usecase.ProcessingDistributionViewImpl;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockCurrentProcDist;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetHeadcountEntityInput;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.SIMULATION;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHeadcountEntityUseCaseTest {

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @Mock
    private CurrentProcessingDistributionRepository currentRepository;

    @Mock
    private GetForecastUseCase getForecastUseCase;

    @InjectMocks
    private GetHeadcountEntityUseCase getHeadcountEntityUseCase;

    private static Stream<Arguments> getSupportedEntitites() {
        return Stream.of(
                Arguments.of(HEADCOUNT, true),
                Arguments.of(PRODUCTIVITY, false),
                Arguments.of(THROUGHPUT, false)
        );
    }

    @Test
    @DisplayName("Get headcount entity when source is forecast")
    public void testGetHeadcountOk() {
        // GIVEN
        final GetHeadcountInput input = mockGetHeadcountEntityInput(FORECAST,
                Set.of(ProcessingType.ACTIVE_WORKERS, ProcessingType.WORKERS), null);
        final List<Long> forecastIds = List.of(1L);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())).thenReturn(forecastIds);

        when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(

                Set.of(ProcessingType.ACTIVE_WORKERS.name(), ProcessingType.WORKERS.name()),
                List.of(PICKING.name(), PACKING.name()),
                A_DATE_UTC, A_DATE_UTC.plusDays(2),
                forecastIds)
        ).thenReturn(processingDistributions());

        // WHEN
        final List<EntityOutput> output = getHeadcountEntityUseCase.execute(input);

        // THEN
        verifyZeroInteractions(currentRepository);
        final EntityOutput output1 = output.get(0);
        assertEquals(A_DATE_UTC.toInstant(), output1.getDate().toInstant());
        assertEquals(PICKING, output1.getProcessName());
        assertEquals(100, output1.getValue());
        assertEquals(WORKERS, output1.getMetricUnit());
        assertEquals(FORECAST, output1.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output1.getWorkflow());

        final EntityOutput output2 = output.get(1);
        assertEquals(A_DATE_UTC.plusHours(1).toInstant(), output2.getDate().toInstant());
        assertEquals(PICKING, output2.getProcessName());
        assertEquals(120, output2.getValue());
        assertEquals(WORKERS, output2.getMetricUnit());
        assertEquals(FORECAST, output2.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output2.getWorkflow());
    }

    @Test
    @DisplayName("Get headcount entity when source is null and has simulations applied")
    public void testGetHeadcountWithUnsavedSimulationOk() {
        // GIVEN
        final GetHeadcountInput input = mockGetHeadcountEntityInput(null,
                Set.of(ProcessingType.ACTIVE_WORKERS, ProcessingType.WORKERS),
                List.of(new Simulation(
                        PICKING,
                        List.of(new SimulationEntity(
                                HEADCOUNT,
                                List.of(new QuantityByDate(A_DATE_UTC, 50))))),
                new Simulation(
                        PACKING,
                        List.of(new SimulationEntity(
                                HEADCOUNT,
                                List.of(new QuantityByDate(A_DATE_UTC, 100)))))));

        final List<Long> forecastIds = List.of(1L);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())).thenReturn(forecastIds);

        when(currentRepository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND,
                ProcessingType.ACTIVE_WORKERS,
                List.of(PICKING, PACKING),
                input.getDateFrom(),
                input.getDateTo()
        )).thenReturn(List.of(
                mockCurrentProcDist(A_DATE_UTC, 40L),
                mockCurrentProcDist(A_DATE_UTC.plusHours(1), 60L)
        ));

        when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                Set.of(ProcessingType.ACTIVE_WORKERS.name(), ProcessingType.WORKERS.name()),
                List.of(PICKING.name(), PACKING.name()),
                A_DATE_UTC, A_DATE_UTC.plusDays(2),
                forecastIds)
        ).thenReturn(processingDistributions());

        // WHEN
        final List<EntityOutput> output = getHeadcountEntityUseCase.execute(input);

        // THEN
        assertEquals(6, output.size());
        final EntityOutput output1 = output.get(0);
        assertEquals(A_DATE_UTC.toInstant(), output1.getDate().toInstant());
        assertEquals(PICKING, output1.getProcessName());
        assertEquals(100, output1.getValue());
        assertEquals(WORKERS, output1.getMetricUnit());
        assertEquals(FORECAST, output1.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output1.getWorkflow());

        final EntityOutput output2 = output.get(1);
        assertEquals(A_DATE_UTC.plusHours(1).toInstant(), output2.getDate().toInstant());
        assertEquals(PICKING, output2.getProcessName());
        assertEquals(120, output2.getValue());
        assertEquals(WORKERS, output2.getMetricUnit());
        assertEquals(FORECAST, output2.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output2.getWorkflow());

        final EntityOutput output3 = output.get(2);
        assertEquals(A_DATE_UTC.plusHours(2).toInstant(), output3.getDate().toInstant());
        assertEquals(PACKING, output3.getProcessName());
        assertEquals(120, output3.getValue());
        assertEquals(WORKERS, output3.getMetricUnit());
        assertEquals(FORECAST, output3.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output3.getWorkflow());

        final EntityOutput output4 = output.get(3);
        assertEquals(A_DATE_UTC.plusHours(1).toInstant(), output4.getDate().toInstant());
        assertEquals(PACKING, output4.getProcessName());
        assertEquals(60, output4.getValue());
        assertEquals(WORKERS, output4.getMetricUnit());
        assertEquals(SIMULATION, output4.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output4.getWorkflow());

        final EntityOutput output5 = output.get(4);
        assertEquals(A_DATE_UTC.toInstant(), output5.getDate().toInstant());
        assertEquals(PICKING, output5.getProcessName());
        assertEquals(50, output5.getValue());
        assertEquals(WORKERS, output5.getMetricUnit());
        assertEquals(SIMULATION, output5.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output5.getWorkflow());

        final EntityOutput output6 = output.get(5);
        assertEquals(A_DATE_UTC.toInstant(), output6.getDate().toInstant());
        assertEquals(PACKING, output6.getProcessName());
        assertEquals(100, output6.getValue());
        assertEquals(WORKERS, output6.getMetricUnit());
        assertEquals(SIMULATION, output6.getSource());
        assertEquals(FBM_WMS_OUTBOUND, output6.getWorkflow());
    }

    @Test
    @DisplayName("Get headcount entity when source is simulation")
    public void testGetHeadcountFromSourceSimulation() {
        // GIVEN
        final GetHeadcountInput input = mockGetHeadcountEntityInput(SIMULATION);
        final List<Long> forecastIds = singletonList(1L);

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(input.getWorkflow())
                .warehouseId(input.getWarehouseId())
                .dateFrom(input.getDateFrom())
                .dateTo(input.getDateTo())
                .build())).thenReturn(forecastIds);

        when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                null,
                input.getProcessNamesAsString(),
                input.getDateFrom(),
                input.getDateTo(),
                forecastIds)
        ).thenReturn(processingDistributions());

        when(currentRepository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                input.getWarehouseId(),
                input.getWorkflow(),
                ProcessingType.ACTIVE_WORKERS,
                input.getProcessName(),
                input.getDateFrom(),
                input.getDateTo())
        ).thenReturn(currentDistribution());

        // WHEN
        final List<EntityOutput> output = getHeadcountEntityUseCase.execute(input);

        // THEN
        assertEquals(5, output.size());
        outputPropertiesEqualTo(output.get(0), FORECAST, 100, A_DATE_UTC.toInstant());
        outputPropertiesEqualTo(output.get(1), FORECAST, 120, A_DATE_UTC.plusHours(1).toInstant());
        outputPropertiesEqualTo(output.get(2), FORECAST, 120, A_DATE_UTC.plusHours(2).toInstant());
        outputPropertiesEqualTo(output.get(3), SIMULATION, 20, A_DATE_UTC.toInstant());
        outputPropertiesEqualTo(output.get(4), SIMULATION, 20, A_DATE_UTC.plusHours(1).toInstant());
    }

    private List<ProcessingDistributionView> processingDistributions() {
        return List.of(
                new ProcessingDistributionViewImpl(
                        1,
                        Date.from(A_DATE_UTC.toInstant()),
                        PICKING,
                        100,
                        WORKERS,
                        ProcessingType.ACTIVE_WORKERS),
                new ProcessingDistributionViewImpl(
                        2,
                        Date.from(A_DATE_UTC.plusHours(1).toInstant()),
                        PICKING,
                        120,
                        WORKERS,
                        ProcessingType.ACTIVE_WORKERS),
                new ProcessingDistributionViewImpl(
                        3,
                        Date.from(A_DATE_UTC.plusHours(2).toInstant()),
                        PACKING,
                        120,
                        WORKERS,
                        ProcessingType.ACTIVE_WORKERS)
        );
    }

    private List<CurrentProcessingDistribution> currentDistribution() {
        return List.of(
                CurrentProcessingDistribution.builder()
                        .date(A_DATE_UTC)
                        .type(ProcessingType.ACTIVE_WORKERS)
                        .quantity(20)
                        .isActive(true)
                        .quantityMetricUnit(WORKERS)
                        .processName(PICKING)
                        .build(),
                CurrentProcessingDistribution.builder()
                        .date(A_DATE_UTC.plusHours(1))
                        .type(ProcessingType.ACTIVE_WORKERS)
                        .quantity(20)
                        .isActive(true)
                        .quantityMetricUnit(WORKERS)
                        .processName(PACKING)
                        .build()
        );
    }

    private void outputPropertiesEqualTo(final EntityOutput entityOutput,
                                         final Source source,
                                         final int quantity,
                                         final Instant instant) {

        assertEquals(source, entityOutput.getSource());
        assertEquals(quantity, entityOutput.getValue());
        assertEquals(instant, entityOutput.getDate().toInstant());
    }
}
