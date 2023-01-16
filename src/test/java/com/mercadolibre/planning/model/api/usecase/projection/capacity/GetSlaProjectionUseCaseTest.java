package com.mercadolibre.planning.model.api.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PUT_AWAY;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.DateUtils.getCurrentUtcDate;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.CPT;
import static java.time.ZonedDateTime.now;
import static java.time.ZonedDateTime.parse;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetSlaProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetSlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseInboundService;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("PMD.LongVariable")
@ExtendWith(MockitoExtension.class)
class GetSlaProjectionUseCaseTest {

  private static final ZonedDateTime DATE_FROM = parse("2020-01-01T12:00:00Z");

  private static final ZonedDateTime DATE_TO = parse("2020-01-10T12:00:00Z");

  private static final String TIMEZONE = "America/Argentina/Buenos_Aires";

  @InjectMocks
  private GetSlaProjectionUseCase getSlaProjectionUseCase;

  @Mock
  private GetThroughputUseCase getThroughputUseCase;

  @Mock
  private PlannedBacklogService plannedBacklogService;

  @Mock
  private CalculateBacklogProjectionUseCase calculateBacklogProjection;

  @Mock
  private GetCapacityPerHourService getCapacityPerHourService;

  @Mock
  private GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

  @Mock
  private GetSlaByWarehouseInboundService getSlaByWarehouseInboundService;

  @Mock
  private GetCycleTimeService getCycleTimeService;

  private MockedStatic<CalculateCptProjectionUseCase> calculateCptProjection;

  @BeforeEach
  public void setUp() {
    calculateCptProjection = mockStatic(CalculateCptProjectionUseCase.class);
  }

  @AfterEach
  public void tearDown() {
    calculateCptProjection.close();
  }

  @Test
  public void testGetCptProjection() {
    // GIVEN
    final ZonedDateTime etd = parse("2020-01-01T11:00:00Z");
    final ZonedDateTime projectedTime = parse("2020-01-02T10:00:00Z");

    calculateCptProjection.when(() -> CalculateCptProjectionUseCase.execute(any()))
        .thenReturn(List.of(
            new CptCalculationOutput(etd, projectedTime, 100, 0, 0, emptyList())
        ));

    when(getCapacityPerHourService.execute(eq(FBM_WMS_OUTBOUND), any(List.class)))
        .thenReturn(List.of(
            new CapacityOutput(now().withFixedOffsetZone(),
                UNITS_PER_HOUR, 100)
        ));

    when(getSlaByWarehouseOutboundService.execute(new GetSlaByWarehouseInput(
        WAREHOUSE_ID, DATE_FROM, DATE_TO, emptyList(), TIMEZONE)))
        .thenReturn(emptyList());

    when(getCycleTimeService.execute(new GetCycleTimeInput(WAREHOUSE_ID, List.of(etd))))
        .thenReturn(Map.of(etd, 45L));

    final ZonedDateTime currentDate = getCurrentUtcDate();
    // WHEN
    final List<CptProjectionOutput> result = getSlaProjectionUseCase.execute(getInput(currentDate));

    // THEN
    verify(plannedBacklogService).getExpectedBacklog(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        DATE_FROM,
        DATE_TO,
        currentDate,
        false
    );

    verify(getThroughputUseCase).execute(any(GetEntityInput.class));
    verifyNoInteractions(calculateBacklogProjection);

    assertNotNull(result);
    assertEquals(1, result.size());

    final var first = result.get(0);
    assertEquals(etd, first.getDate());
    assertEquals(projectedTime, first.getProjectedEndDate());
    assertEquals(100, first.getRemainingQuantity());
    assertEquals(45L, first.getProcessingTime().getValue());
  }

  @Test
  public void testGetSlaProjection() {
    // GIVEN
    final ZonedDateTime etd = parse("2020-01-01T11:00:00Z");
    final ZonedDateTime projectedTime = parse("2020-01-02T10:00:00Z");

    final var entities = List.of(
        EntityOutput.builder().workflow(FBM_WMS_INBOUND).date(DATE_FROM).processName(PUT_AWAY).quantity(5).build(),
        EntityOutput.builder().workflow(FBM_WMS_INBOUND).date(DATE_FROM).processName(PUT_AWAY).quantity(10).build()
    );

    when(getThroughputUseCase.execute(any(GetEntityInput.class)))
        .thenReturn(entities);

    when(getCapacityPerHourService.execute(eq(FBM_WMS_INBOUND), any(List.class)))
        .thenReturn(List.of(
            new CapacityOutput(DATE_FROM, null, 10),
            new CapacityOutput(DATE_FROM.plusHours(1), null, 20)
        ));

    when(plannedBacklogService.getExpectedBacklog(WAREHOUSE_ID, FBM_WMS_INBOUND, DATE_FROM, DATE_TO,
        parse("2020-01-10T12:00Z"), false))
        .thenReturn(List.of(
            new PlannedUnits(DATE_FROM, DATE_FROM.plusHours(1), 10)
        ));

    when(getSlaByWarehouseInboundService.execute(any(GetSlaByWarehouseInput.class)))
        .thenReturn(List.of(
            GetSlaByWarehouseOutput.builder().date(DATE_FROM).build(),
            GetSlaByWarehouseOutput.builder().date(DATE_FROM.plusHours(1)).build()
        ));

    calculateCptProjection.when(() -> CalculateCptProjectionUseCase.execute(any()))
        .thenReturn(List.of(
            new CptCalculationOutput(etd, projectedTime, 100, 0, 0, emptyList())
        ));

    // WHEN
    final List<CptProjectionOutput> result = getSlaProjectionUseCase.execute(getInboundInput());

    // THEN
    assertNotNull(result);
    assertEquals(1, result.size());

    final var first = result.get(0);
    assertEquals(etd, first.getDate());
    assertEquals(projectedTime, first.getProjectedEndDate());
    assertEquals(100, first.getRemainingQuantity());

    verifyNoInteractions(getSlaByWarehouseOutboundService);
    verifyNoInteractions(getCycleTimeService);
  }

  private GetSlaProjectionInput getInput(final ZonedDateTime currentDate) {
    return new GetSlaProjectionInput(
        FBM_WMS_OUTBOUND,
        WAREHOUSE_ID,
        CPT,
        List.of(ProcessName.PICKING, ProcessName.PACKING),
        DATE_FROM,
        DATE_TO,
        null,
        TIMEZONE,
        null,
        null,
        false,
        currentDate
    );
  }

  private GetSlaProjectionInput getInboundInput() {
    return new GetSlaProjectionInput(
        FBM_WMS_INBOUND,
        WAREHOUSE_ID,
        CPT,
        List.of(PUT_AWAY),
        DATE_FROM,
        DATE_TO,
        List.of(new QuantityByDate(DATE_FROM, 10)),
        TIMEZONE,
        null,
        null,
        false,
        parse("2020-01-10T12:00:00Z")
    );
  }
}
