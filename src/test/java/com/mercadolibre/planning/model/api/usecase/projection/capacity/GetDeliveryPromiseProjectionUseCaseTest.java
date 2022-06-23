package com.mercadolibre.planning.model.api.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.GLOBAL;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.MAX_CAPACITY;
import static com.mercadolibre.planning.model.api.web.controller.entity.EntityType.THROUGHPUT;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.ProcessingDistributionView;
import com.mercadolibre.planning.model.api.domain.entity.Workflow;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
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
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.DeferralStatus;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeliveryPromiseProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeliveryPromiseProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.usecase.ProcessingDistributionViewImpl;
import com.mercadolibre.planning.model.api.util.DateUtils;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import com.mercadolibre.planning.model.api.web.controller.simulation.Simulation;
import com.mercadolibre.planning.model.api.web.controller.simulation.SimulationEntity;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

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
  private ProcessingDistributionRepository processingDistRepository;

  @Mock
  private GetForecastUseCase getForecastUseCase;

  @Mock
  private GetCycleTimeService getCycleTimeService;

  @Mock
  private GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

  private MockedStatic<CalculateCptProjectionUseCase> calculateCptProjection;

  private MockedStatic<DateUtils> dateUtils;

  public static Stream<Arguments> argOfTestProjectionDeliveryPromise() {
    return Stream.of(projectionOk(), projectionNpe());
  }

  private static Arguments projectionOk() {
    final var processingTime = new ProcessingTime(360L, MINUTES);
    return Arguments.of(
        "TestValues",
        getSlas(),
        List.of(1L, 2L),
        List.of(new Backlog(CPT_1, 100), new Backlog(CPT_2, 200)),
        List.of(CPT_1, CPT_2),
        emptyList(),
        List.of(
            new DeliveryPromiseProjectionOutput(CPT_1, CPT_1.minusHours(2), 0, CPT_1.minusHours(6),
                processingTime, CPT_1.minusHours(7), false, DeferralStatus.NOT_DEFERRED),
            new DeliveryPromiseProjectionOutput(CPT_2, CPT_2.minusHours(2), 0, CPT_2.minusHours(6),
                processingTime, CPT_1.minusHours(7), false, DeferralStatus.NOT_DEFERRED)
        ),
        List.of(new Simulation(
            GLOBAL,
            List.of(new SimulationEntity(
                THROUGHPUT,
                List.of(
                    new QuantityByDate(NOW.truncatedTo(SECONDS),
                        130))
            ))
        ))
    );
  }

  private static Arguments projectionNpe() {
    return Arguments.of(
        "TestSomeFieldNullPointerException",
        getSlas(),
        List.of(1L, 2L),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList()
    );
  }

  private static List<ProcessingDistributionView> processingDistributions() {
    return List.of(
        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(1).toInstant(UTC))).quantity(120L).build(),
        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(2).toInstant(UTC))).quantity(100L).build(),
        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(3).toInstant(UTC))).quantity(130L).build(),
        ProcessingDistributionViewImpl.builder().date(Date.from(NOW.toLocalDateTime().plusHours(5).toInstant(UTC))).quantity(100L).build()
    );
  }

  private static Map<ZonedDateTime, Integer> maxCapacitiesByHours() {
    final var truncatedNow = NOW.truncatedTo(SECONDS);
    return Map.of(
        truncatedNow, 130,
        truncatedNow.plusHours(1), 120,
        truncatedNow.plusHours(2), 100,
        truncatedNow.plusHours(3), 130,
        truncatedNow.plusHours(4), 130,
        truncatedNow.plusHours(5), 100
    );
  }

  private static List<GetSlaByWarehouseOutput> getSlas() {
    return List.of(
        GetSlaByWarehouseOutput.builder().date(CPT_1).processingTime(new ProcessingTime(360L, MINUTES)).build(),
        GetSlaByWarehouseOutput.builder().date(CPT_2).processingTime(new ProcessingTime(360L, MINUTES)).build()
    );
  }

  @BeforeEach
  public void setUp() {
    calculateCptProjection = mockStatic(CalculateCptProjectionUseCase.class);
    dateUtils = mockStatic(DateUtils.class);
    dateUtils.when(DateUtils::getCurrentUtcDate)
        .thenReturn(NOW.truncatedTo(SECONDS));
  }

  @AfterEach
  public void tearDown() {
    calculateCptProjection.close();
    dateUtils.close();
  }

  @ParameterizedTest
  @MethodSource("argOfTestProjectionDeliveryPromise")
  public void testProjectionDeliveryPromise(final String assertionsGroup,
                                            final List<GetSlaByWarehouseOutput> cptByWarehouse,
                                            final List<Long> forecastIds,
                                            final List<Backlog> backlogs,
                                            final List<ZonedDateTime> cptDatesFromBacklog,
                                            final List<CptCalculationDetailOutput> cptCalculationDetails,
                                            final List<DeliveryPromiseProjectionOutput> projectionExpected,
                                            final List<Simulation> simulations) {
    //GIVEN
    final ZonedDateTime dateFrom = NOW;
    final ZonedDateTime dateTo = NOW.plusHours(6);
    final String logisticCenterId = WAREHOUSE_ID;
    final Workflow workflow = FBM_WMS_OUTBOUND;
    final List<ProcessingDistributionView> maxCapacitiesPlanned = processingDistributions();
    final Map<ZonedDateTime, Integer> maxCapacitiesByHours = maxCapacitiesByHours();
    final Map<ZonedDateTime, Long> cycleTimeByCpt = Map.of(CPT_1, 360L, CPT_2, 360L);

    when(getSlaByWarehouseOutboundService.execute(
        new GetSlaByWarehouseInput(logisticCenterId, dateFrom, dateTo, cptDatesFromBacklog, null))).thenReturn(cptByWarehouse);

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

    when(getCycleTimeService.execute(
        new GetCycleTimeInput(
            logisticCenterId,
            List.of(CPT_1, CPT_2))
    )).thenReturn(cycleTimeByCpt);

    calculateCptProjection.when(() ->
        CalculateCptProjectionUseCase.execute(
            SlaProjectionInput.builder()
                .capacity(maxCapacitiesByHours)
                .backlog(backlogs)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .plannedUnits(emptyList())
                .slaByWarehouse(cptByWarehouse)
                .currentDate(NOW.truncatedTo(SECONDS))
                .build()
        )
    ).thenReturn(
        projectionExpected.stream()
            .map(item ->
                new CptCalculationOutput(
                    item.getDate(),
                    item.getProjectedEndDate(),
                    item.getRemainingQuantity(),
                    0,
                    0,
                    cptCalculationDetails)
            )
            .collect(Collectors.toList())
    );

    //WHEN
    final List<DeliveryPromiseProjectionOutput> projectionResult =
        getDeliveryPromiseUseCase.execute(GetDeliveryPromiseProjectionInput.builder()
            .warehouseId(logisticCenterId)
            .workflow(workflow)
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .backlog(backlogs)
            .simulations(simulations)
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
}
