package com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.CPT;
import static java.time.ZonedDateTime.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetSlaProjectionInput;
import com.mercadolibre.planning.model.api.web.controller.projection.request.QuantityByDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueueProjectionServiceTest {

  private static final String TIMEZONE = "America/Argentina/Buenos_Aires";

  private static final String WAREHOUSE_ID = "ARTW01";

  private static final ZonedDateTime VIEW_DATE = parse("2022-05-10T10:15:00Z");

  private static final ZonedDateTime DATE_FROM = parse("2022-05-10T10:00:00Z");

  private static final ZonedDateTime DATE_TO = parse("2022-05-20T11:00:00Z");

  @InjectMocks
  QueueProjectionService queueProjectionService;

  @Mock
  private GetThroughputUseCase getThroughputUseCase;

  @Mock
  private GetCapacityPerHourService getCapacityPerHourService;

  @Mock
  private PlannedBacklogService plannedBacklogService;

  @Mock
  private GetCycleTimeService getCycleTimeService;

  @Test
  void projectionSlaOk() {

    //GIVEN
    when(plannedBacklogService.getExpectedBacklog(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND, DATE_FROM, DATE_TO, VIEW_DATE, false)).thenReturn(List.of());

    when(getCapacityPerHourService.execute(eq(FBM_WMS_OUTBOUND), any(List.class)))
        .thenReturn(List.of(
            new CapacityOutput(DATE_FROM.plusHours(1), UNITS_PER_HOUR, 10),
            new CapacityOutput(DATE_FROM.plusHours(2), UNITS_PER_HOUR, 10),
            new CapacityOutput(DATE_FROM.plusHours(3), UNITS_PER_HOUR, 8)
        ));

    when(getCycleTimeService.execute(
        new GetCycleTimeInput(
            WAREHOUSE_ID,
            List.of(
                DATE_FROM.plusMinutes(30),
                DATE_FROM.plusHours(1),
                DATE_FROM.plusHours(2),
                DATE_FROM.plusHours(3))
        )
    ))
        .thenReturn(Map.of(DATE_FROM.plusHours(1), 20L, DATE_FROM.plusHours(2), 20L, DATE_FROM.plusHours(3), 20L));

    //WHEN
    final List<CptProjectionOutput> result = queueProjectionService.calculateCptProjection(getInput());

    // THEN
    assertEquals(4, result.size());
    assertEquals(10, result.get(0).getRemainingQuantity());
    assertEquals(5, result.get(1).getRemainingQuantity());
    assertEquals(20, result.get(2).getRemainingQuantity());
    assertEquals(30, result.get(3).getRemainingQuantity());
  }

  @Test
  void projectionSlaWithEmptyBacklogsOk() {

    //GIVEN
    when(plannedBacklogService.getExpectedBacklog(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND, DATE_FROM, DATE_TO, VIEW_DATE, false)).thenReturn(List.of());

    when(getCapacityPerHourService.execute(eq(FBM_WMS_OUTBOUND), any(List.class)))
        .thenReturn(List.of(
            new CapacityOutput(DATE_FROM.plusHours(0), UNITS_PER_HOUR, 6),
            new CapacityOutput(DATE_FROM.plusHours(1), UNITS_PER_HOUR, 10),
            new CapacityOutput(DATE_FROM.plusHours(2), UNITS_PER_HOUR, 10),
            new CapacityOutput(DATE_FROM.plusHours(3), UNITS_PER_HOUR, 8)
        ));

    when(getCycleTimeService.execute(new GetCycleTimeInput(WAREHOUSE_ID, List.of())))
        .thenReturn(Map.of());

    //WHEN
    final List<CptProjectionOutput> result = queueProjectionService.calculateCptProjection(new GetSlaProjectionInput(
            FBM_WMS_OUTBOUND,
            WAREHOUSE_ID,
            CPT,
            List.of(ProcessName.PICKING, ProcessName.PACKING),
            DATE_FROM,
            DATE_TO,
            List.of(),
            TIMEZONE,
            null,
            null,
            false,
            VIEW_DATE
        )
    );

    // THEN
    assertEquals(0, result.size());
  }

  @Test
  void projectionSlaWithUnitsConsumptionOk() {

    //GIVEN
    when(plannedBacklogService.getExpectedBacklog(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND, DATE_FROM, DATE_TO, VIEW_DATE, false)).thenReturn(List.of());

    when(getCapacityPerHourService.execute(eq(FBM_WMS_OUTBOUND), any(List.class)))
        .thenReturn(List.of(
            new CapacityOutput(DATE_FROM.plusHours(0), UNITS_PER_HOUR, 20),
            new CapacityOutput(DATE_FROM.plusHours(1), UNITS_PER_HOUR, 20),
            new CapacityOutput(DATE_FROM.plusHours(2), UNITS_PER_HOUR, 10),
            new CapacityOutput(DATE_FROM.plusHours(3), UNITS_PER_HOUR, 10)
        ));

    when(getCycleTimeService.execute(
        new GetCycleTimeInput(
            WAREHOUSE_ID,
            List.of(
                DATE_FROM.plusMinutes(30),
                DATE_FROM.plusHours(1),
                DATE_FROM.plusHours(2),
                DATE_FROM.plusHours(3))
        )
    ))
        .thenReturn(Map.of(DATE_FROM, 0L, DATE_FROM.plusHours(1), 0L, DATE_FROM.plusHours(2), 0L, DATE_FROM.plusHours(3), 0L));

    //WHEN
    final List<CptProjectionOutput> result = queueProjectionService.calculateCptProjection(getInput());

    // THEN
    assertEquals(4, result.size());
    assertEquals(10, result.get(0).getRemainingQuantity());
    assertEquals(0, result.get(1).getRemainingQuantity());
    assertNotNull(result.get(1).getDate());
  }

  @Test
  void projectionWhenTooLargeBacklogOk() {

    //GIVEN
    when(plannedBacklogService.getExpectedBacklog(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND, DATE_FROM, DATE_TO,  VIEW_DATE, false)).thenReturn(List.of());

    when(getCapacityPerHourService.execute(eq(FBM_WMS_OUTBOUND), any(List.class)))
        .thenReturn(List.of(
            new CapacityOutput(DATE_FROM.plusHours(0), UNITS_PER_HOUR, 6),
            new CapacityOutput(DATE_FROM.plusHours(1), UNITS_PER_HOUR, 10),
            new CapacityOutput(DATE_FROM.plusHours(2), UNITS_PER_HOUR, 10),
            new CapacityOutput(DATE_FROM.plusHours(3), UNITS_PER_HOUR, 8)
        ));

    when(getCycleTimeService.execute(
        new GetCycleTimeInput(
            WAREHOUSE_ID,
            List.of(DATE_FROM, DATE_FROM.plusHours(1), DATE_FROM.plusHours(2), DATE_FROM.plusHours(3))
        )
    ))
        .thenReturn(Map.of(DATE_FROM, 20L, DATE_FROM.plusHours(1), 20L, DATE_FROM.plusHours(2), 20L, DATE_FROM.plusHours(3), 20L));

    //WHEN
    final List<CptProjectionOutput> result = queueProjectionService.calculateCptProjection(new GetSlaProjectionInput(
        FBM_WMS_OUTBOUND,
        WAREHOUSE_ID,
        CPT,
        List.of(ProcessName.PICKING, ProcessName.PACKING),
        DATE_FROM,
        DATE_TO,
        List.of(
            new QuantityByDate(DATE_FROM.plusHours(0), 50000, null),
            new QuantityByDate(DATE_FROM.plusHours(1), 50000, null),
            new QuantityByDate(DATE_FROM.plusHours(2), 50000, null),
            new QuantityByDate(DATE_FROM.plusHours(3), 50000, null)
        ),
        TIMEZONE,
        null,
        null,
        false,
        VIEW_DATE
    ));

    // THEN


    assertEquals(4, result.size());
    assertEquals(50000, result.get(0).getRemainingQuantity());
  }

  private GetSlaProjectionInput getInput() {
    return new GetSlaProjectionInput(
        FBM_WMS_OUTBOUND,
        WAREHOUSE_ID,
        CPT,
        List.of(ProcessName.PICKING, ProcessName.PACKING),
        DATE_FROM,
        DATE_TO,
        List.of(
            new QuantityByDate(DATE_FROM.plusMinutes(30), 10, null),
            new QuantityByDate(DATE_FROM.plusHours(1), 5, null),
            new QuantityByDate(DATE_FROM.plusHours(2), 20, null),
            new QuantityByDate(DATE_FROM.plusHours(3), 30, null)
        ),
        TIMEZONE,
        null,
        null,
        false,
        VIEW_DATE
    );
  }
}
