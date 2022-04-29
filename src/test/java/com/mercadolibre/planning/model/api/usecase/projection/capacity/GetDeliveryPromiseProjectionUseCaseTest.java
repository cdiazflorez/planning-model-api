package com.mercadolibre.planning.model.api.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.COMMAND_CENTER_DEFERRAL;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationDetailOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.DeliveryPromiseProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.SlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeliveryPromiseProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.usecase.ProcessingDistributionViewImpl;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// TODO this test fails sporadically depending on the clock.
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LongVariable"})
@ExtendWith(MockitoExtension.class)
public class GetDeliveryPromiseProjectionUseCaseTest {

    private static final String WAREHOUSE_ID = "ARBA01";

    private static final ZonedDateTime NOW = ZonedDateTime.now(UTC);

    private static final ZonedDateTime CPT_1 = NOW.plusHours(1);

    private static final ZonedDateTime CPT_2 = NOW.plusHours(2);

    @InjectMocks
    private GetDeliveryPromiseProjectionUseCase getDeliveryPromiseUseCase;

    @Mock
    private CalculateCptProjectionUseCase calculatedProjectionUseCase;

    @Mock
    private ProcessingDistributionRepository processingDistRepository;

    @Mock
    private GetForecastUseCase getForecastUseCase;

    @Mock
    private GetCycleTimeService getCycleTimeService;

    @Mock
    private GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

    @Mock
    private PlannedBacklogService plannedBacklogService;

    @ParameterizedTest
    @MethodSource("argOfTestProjectionDeliveryPromise")
    public void testProjectionDeliveryPromise(final String assertionsGroup,
                                              final Workflow workflow,
                                              final String logisticCenterId,
                                              final ZonedDateTime dateFrom,
                                              final ZonedDateTime dateTo,
                                              final ProjectionType type,
                                              final List<GetSlaByWarehouseOutput> cptByWarehouse,
                                              final List<Long> forecastIds,
                                              final List<Backlog> backlogs,
                                              final List<ZonedDateTime> cptDatesFromBacklog,
                                              final Map<ZonedDateTime, Long> cycleTimeByCpt,
                                              final List<ProcessingDistributionView> maxCapacitiesPlanned,
                                              final Map<ZonedDateTime, Integer> maxCapacitiesByHours,
                                              final List<CptCalculationDetailOutput> cptCalculationDetails,
                                              final List<DeliveryPromiseProjectionOutput> projectionExpected) {
        //GIVEN
        when(getSlaByWarehouseOutboundService.execute(
                new GetSlaByWarehouseInput(logisticCenterId, dateFrom, dateTo, cptDatesFromBacklog, null))).thenReturn(cptByWarehouse);

        if (type != null) {
            when(plannedBacklogService.getExpectedBacklog(logisticCenterId, workflow, dateFrom, dateTo, false))
                    .thenReturn(emptyList());
        }

        when(getForecastUseCase.execute(GetForecastInput.builder()
                .workflow(workflow)
                .warehouseId(logisticCenterId)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .build())).thenReturn(forecastIds);

        when(processingDistRepository.findByWarehouseIdWorkflowTypeProcessNameAndDateInRange(
                Set.of(MAX_CAPACITY.name()),
                List.of(GLOBAL.toJson()),
                dateFrom,
                dateTo,
                forecastIds)).thenReturn(maxCapacitiesPlanned);

        when(calculatedProjectionUseCase.execute(SlaProjectionInput.builder()
                .workflow(workflow)
                .logisticCenterId(logisticCenterId)
                .capacity(maxCapacitiesByHours)
                .backlog(backlogs)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .plannedUnits(emptyList())
                .slaByWarehouse(cptByWarehouse)
                // TODO handle the time correctly: sometimes this test fails because the current date changes.
                .currentDate(getCurrentUtcDate())
                .build()))
                .thenReturn(projectionExpected.stream().map(item ->
                                new CptCalculationOutput(item.getDate(),
                                        item.getProjectedEndDate(),
                                        item.getRemainingQuantity(), 0, 0, cptCalculationDetails))
                        .collect(Collectors.toList()));

        when(getCycleTimeService.execute(
                new GetCycleTimeInput(logisticCenterId, projectionExpected.stream().map(DeliveryPromiseProjectionOutput::getDate).collect(
                        Collectors.toList())))).thenReturn(cycleTimeByCpt);

        //WHEN
        final List<DeliveryPromiseProjectionOutput> projectionResult =
                getDeliveryPromiseUseCase.execute(GetDeliveryPromiseProjectionInput.builder()
                        .warehouseId(logisticCenterId)
                        .workflow(workflow)
                        .dateFrom(dateFrom)
                        .dateTo(dateTo)
                        .backlog(backlogs)
                        .projectionType(type)
                        .build());

        //THEN
        if ("TestValues".equals(assertionsGroup)) {
            assertEquals(projectionExpected.size(), projectionResult.size());
            assertEquals(projectionExpected.get(0).getDate(), projectionResult.get(0).getDate());
            assertEquals(projectionExpected.get(0).getProjectedEndDate(), projectionResult.get(0).getProjectedEndDate());
            assertEquals(projectionExpected.get(0).getRemainingQuantity(), projectionResult.get(0).getRemainingQuantity());
            assertEquals(projectionExpected.get(0).getEtdCutoff(), projectionResult.get(0).getEtdCutoff());
            assertEquals(projectionExpected.get(0).isDeferred(), projectionResult.get(0).isDeferred());
            assertEquals(projectionExpected.get(0).getProcessingTime(), projectionResult.get(0).getProcessingTime());
        }
        if ("TestSomeFieldNullPointerException".equals(assertionsGroup)) {
            assertEquals(projectionExpected.isEmpty(), projectionResult.isEmpty());
        }
    }

    public static Stream<Arguments> argOfTestProjectionDeliveryPromise() {
        return Stream.of(projectionOk(), projectionNpe(), projectionDeferralCommandCenter());
    }

    private static Arguments projectionOk() {
        return Arguments.of(
                "TestValues",
                FBM_WMS_OUTBOUND,
                WAREHOUSE_ID,
                NOW,
                NOW.plusHours(6),
                null,
                List.of(GetSlaByWarehouseOutput.builder().date(CPT_1).processingTime(new ProcessingTime(360L, MINUTES)).build(),
                        GetSlaByWarehouseOutput.builder().date(CPT_2).processingTime(new ProcessingTime(360L, MINUTES)).build()),
                List.of(1L, 2L),
                List.of(new Backlog(CPT_1, 100), new Backlog(CPT_2, 200)),
                List.of(CPT_1, CPT_2),
                Map.of(CPT_1, 360L, CPT_2, 360L),
                List.of(ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(1).toInstant(UTC)))
                                .quantity(120L).build(),
                        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(2).toInstant(UTC)))
                                .quantity(100L).build(),
                        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(3).toInstant(UTC)))
                                .quantity(130L).build(),
                        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(5).toInstant(UTC)))
                                .quantity(100L).build()
                ),
                Map.of(NOW.truncatedTo(SECONDS), 130,
                        NOW.plusHours(1).truncatedTo(SECONDS), 120,
                        NOW.plusHours(2).truncatedTo(SECONDS), 100,
                        NOW.plusHours(3).truncatedTo(SECONDS), 130,
                        NOW.plusHours(4).truncatedTo(SECONDS), 130,
                        NOW.plusHours(5).truncatedTo(SECONDS), 100),
                emptyList(),
                List.of(new DeliveryPromiseProjectionOutput(CPT_1, CPT_1.minusHours(2), 0, CPT_1.minusHours(6),
                                new ProcessingTime(360L, MINUTES), CPT_1.minusHours(7), false, 0),
                        new DeliveryPromiseProjectionOutput(CPT_2, CPT_2.minusHours(2), 0, CPT_2.minusHours(6),
                                new ProcessingTime(360L, MINUTES), CPT_1.minusHours(7), false, 0)
                )
        );
    }

    private static Arguments projectionNpe() {
        return Arguments.of(
                "TestSomeFieldNullPointerException",
                FBM_WMS_OUTBOUND,
                WAREHOUSE_ID,
                NOW,
                NOW.plusHours(6),
                null,
                List.of(GetSlaByWarehouseOutput.builder().date(CPT_1).processingTime(new ProcessingTime(360L, MINUTES)).build(),
                        GetSlaByWarehouseOutput.builder().date(CPT_2).processingTime(new ProcessingTime(360L, MINUTES)).build()),
                List.of(1L, 2L),
                emptyList(),
                emptyList(),
                Map.of(CPT_1, 360L, CPT_2, 360L),
                List.of(ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(1).toInstant(UTC)))
                                .quantity(120L).build(),
                        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(2).toInstant(UTC)))
                                .quantity(100L).build(),
                        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(3).toInstant(UTC)))
                                .quantity(130L).build(),
                        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(5).toInstant(UTC)))
                                .quantity(100L).build()
                ),
                Map.of(NOW.truncatedTo(SECONDS), 130,
                        NOW.plusHours(1).truncatedTo(SECONDS), 120,
                        NOW.plusHours(2).truncatedTo(SECONDS), 100,
                        NOW.plusHours(3).truncatedTo(SECONDS), 130,
                        NOW.plusHours(4).truncatedTo(SECONDS), 130,
                        NOW.plusHours(5).truncatedTo(SECONDS), 100),
                emptyList(),
                emptyList()
        );
    }

    private static Arguments projectionDeferralCommandCenter() {
        return Arguments.of(
                "TestValues",
                FBM_WMS_OUTBOUND,
                WAREHOUSE_ID,
                NOW,
                NOW.plusHours(6),
                COMMAND_CENTER_DEFERRAL,
                List.of(GetSlaByWarehouseOutput.builder().date(CPT_1).processingTime(new ProcessingTime(15L, MINUTES)).build(),
                        GetSlaByWarehouseOutput.builder().date(CPT_2).processingTime(new ProcessingTime(15L, MINUTES)).build()),
                List.of(1L, 2L),
                List.of(new Backlog(CPT_1, 100), new Backlog(CPT_2, 200)),
                List.of(CPT_1, CPT_2),
                Map.of(CPT_1, 15L, CPT_2, 15L),
                List.of(ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(1).toInstant(UTC)))
                                .quantity(120L).build(),
                        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(2).toInstant(UTC)))
                                .quantity(100L).build(),
                        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(3).toInstant(UTC)))
                                .quantity(130L).build(),
                        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(5).toInstant(UTC)))
                                .quantity(100L).build()
                ),
                Map.of(NOW.truncatedTo(SECONDS), 130,
                        NOW.plusHours(1).truncatedTo(SECONDS), 120,
                        NOW.plusHours(2).truncatedTo(SECONDS), 100,
                        NOW.plusHours(3).truncatedTo(SECONDS), 130,
                        NOW.plusHours(4).truncatedTo(SECONDS), 130,
                        NOW.plusHours(5).truncatedTo(SECONDS), 100),
                List.of(new CptCalculationDetailOutput(NOW.truncatedTo(ChronoUnit.HOURS), 0, 50000),
                        new CptCalculationDetailOutput(NOW.plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS), 0, 50000),
                        new CptCalculationDetailOutput(NOW.plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS), 0, 50000)),
                List.of(new DeliveryPromiseProjectionOutput(CPT_1, null, 0, CPT_1.minusMinutes(15),
                                new ProcessingTime(15L, MINUTES), CPT_1.minusHours(7), true, 0),
                        new DeliveryPromiseProjectionOutput(CPT_2, null, 0, CPT_2.minusMinutes(15),
                                new ProcessingTime(15L, MINUTES), CPT_1.minusHours(7), true, 0)
                )
        );
    }
}
