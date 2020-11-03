package com.mercadolibre.planning.model.api.usecase;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentProcessingDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.GetHeadcountEntityUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.input.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.output.EntityOutput;
import com.mercadolibre.planning.model.api.web.controller.request.EntityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PICKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.ACTIVE_WORKERS;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.getForecastWeeks;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockGetHeadcountEntityInput;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.HEADCOUNT;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.PRODUCTIVITY;
import static com.mercadolibre.planning.model.api.web.controller.request.EntityType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.FORECAST;
import static com.mercadolibre.planning.model.api.web.controller.request.Source.SIMULATION;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetHeadcountEntityUseCaseTest {

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @Mock
    private CurrentProcessingDistributionRepository currentRepository;

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
        final GetEntityInput input = mockGetHeadcountEntityInput(FORECAST);

        when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                WAREHOUSE_ID,
                FBM_WMS_OUTBOUND.name(),
                ACTIVE_WORKERS.name(),
                List.of(PICKING.name(), PACKING.name()),
                A_DATE_UTC, A_DATE_UTC.plusDays(2),
                getForecastWeeks(A_DATE_UTC, A_DATE_UTC.plusDays(2)))
        ).thenReturn(processingDistributions());

        // WHEN
        final List<EntityOutput> output = getHeadcountEntityUseCase.execute(input);

        // THEN
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
    @DisplayName("Get headcount entity when source is simulation")
    public void testGetHeadcountFromSourceSimulation() {
        // GIVEN
        final GetEntityInput input = mockGetHeadcountEntityInput(SIMULATION);
        when(currentRepository.findSimulationByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                input.getWarehouseId(),
                input.getWorkflow(),
                ACTIVE_WORKERS,
                input.getProcessName(),
                input.getDateFrom(),
                input.getDateTo())
        ).thenReturn(currentDistribution());

        when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                input.getWarehouseId(),
                input.getWorkflow().name(),
                ACTIVE_WORKERS.name(),
                input.getProcessName().stream().map(Enum::name).collect(toList()),
                input.getDateFrom(),
                input.getDateTo(),
                getForecastWeeks(input.getDateFrom(), input.getDateTo()))
        ).thenReturn(processingDistributions());

        // WHEN
        final List<EntityOutput> output = getHeadcountEntityUseCase.execute(input);

        // THEN
        assertEquals(3, output.size());

        final Predicate<EntityOutput> whenIsPickingDist = t ->
                t.getProcessName().name().equals(PICKING.name()) && t.getDate().isEqual(A_DATE_UTC);
        assertEquals(20, output.stream().filter(whenIsPickingDist)
                .findAny().get().getValue());

        final Predicate<EntityOutput> whenIsPAckingDist = t ->
                t.getProcessName().name().equals(PACKING.name())
                        && t.getDate().isEqual(A_DATE_UTC.plusHours(2));
        assertEquals(120, output.stream().filter(whenIsPAckingDist)
                .findAny().get().getValue());
    }

    @ParameterizedTest
    @DisplayName("Only supports headcount entity")
    @MethodSource("getSupportedEntitites")
    public void testSupportEntityTypeOk(final EntityType entityType,
                                 final boolean shouldBeSupported) {
        // WHEN
        final boolean isSupported = getHeadcountEntityUseCase.supportsEntityType(entityType);

        // THEN
        assertEquals(shouldBeSupported, isSupported);
    }

    private List<ProcessingDistributionView> processingDistributions() {
        return List.of(
                new ProcessingDistributionViewImpl(
                        1,
                        Date.from(A_DATE_UTC.toInstant()),
                        PICKING,
                        100,
                        WORKERS,
                        ACTIVE_WORKERS),
                new ProcessingDistributionViewImpl(
                        2,
                        Date.from(A_DATE_UTC.plusHours(1).toInstant()),
                        PICKING,
                        120,
                        WORKERS,
                        ACTIVE_WORKERS),
                new ProcessingDistributionViewImpl(
                        3,
                        Date.from(A_DATE_UTC.plusHours(2).toInstant()),
                        PACKING,
                        120,
                        WORKERS,
                        ACTIVE_WORKERS)
        );
    }

    private List<CurrentProcessingDistribution> currentDistribution() {
        return List.of(
                CurrentProcessingDistribution.builder()
                        .date(A_DATE_UTC)
                        .type(ACTIVE_WORKERS)
                        .quantity(20)
                        .isActive(true)
                        .quantityMetricUnit(WORKERS)
                        .processName(PICKING)
                        .build(),
                CurrentProcessingDistribution.builder()
                        .date(A_DATE_UTC.plusHours(1))
                        .type(ACTIVE_WORKERS)
                        .quantity(20)
                        .isActive(true)
                        .quantityMetricUnit(WORKERS)
                        .processName(PACKING)
                        .build()
        );
    }
}
