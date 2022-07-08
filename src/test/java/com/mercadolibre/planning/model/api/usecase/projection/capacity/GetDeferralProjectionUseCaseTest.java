package com.mercadolibre.planning.model.api.usecase.projection.capacity;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.MINUTES;
import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessName.PACKING_WALL;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessingType.THROUGHPUT;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.Source.FORECAST;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseOutput;
import com.mercadolibre.planning.model.api.domain.entity.sla.ProcessingTime;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeInput;
import com.mercadolibre.planning.model.api.domain.usecase.cycletime.get.GetCycleTimeService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.EntityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.maxcapacity.get.MaxCapacityService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.Backlog;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.DeferralStatus;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetDeferralProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetDeferralProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.output.DeferralProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import com.mercadolibre.planning.model.api.util.DateUtils;
import com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType;
import com.mercadolibre.planning.model.api.web.controller.projection.request.Source;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings({"PMD.ExcessiveImports", "PMD.LongVariable"})
@ExtendWith(MockitoExtension.class)
public class GetDeferralProjectionUseCaseTest {

  private static final String WAREHOUSE_ID = "ARBA01";

  private static final ZonedDateTime NOW = ZonedDateTime.parse("2022-06-08T12:00:00Z");

  private static final ZonedDateTime TRUNCATED_NOW = NOW.truncatedTo(HOURS);

  private static final List<ZonedDateTime> OPERATING_HOURS = of(
      TRUNCATED_NOW,
      TRUNCATED_NOW.plusHours(1),
      TRUNCATED_NOW.plusHours(2),
      TRUNCATED_NOW.plusHours(3),
      TRUNCATED_NOW.plusHours(4),
      TRUNCATED_NOW.plusHours(5),
      TRUNCATED_NOW.plusHours(6),
      TRUNCATED_NOW.plusHours(7),
      TRUNCATED_NOW.plusHours(8)
  );

  private static final List<ZonedDateTime> CPTS = of(
      OPERATING_HOURS.get(4),
      OPERATING_HOURS.get(5),
      OPERATING_HOURS.get(6),
      OPERATING_HOURS.get(7)
  );

  @InjectMocks
  private GetDeferralProjectionUseCase getDeliveryPromiseUseCase;

  @Mock
  private MaxCapacityService maxCapacityService;

  @Mock
  private GetCycleTimeService getCycleTimeService;

  @Mock
  private GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

  @Mock
  private PlannedBacklogService plannedBacklogService;

  @Mock
  private GetThroughputUseCase getThroughputUseCase;

  @Mock
  private GetCapacityPerHourService getCapacityPerHourService;

  private MockedStatic<DateUtils> dateUtils;

  private static List<Backlog> getNoDeferralBacklogs() {
    return of(
        new Backlog(CPTS.get(0), 500),
        new Backlog(CPTS.get(1), 500),
        new Backlog(CPTS.get(2), 1000)
    );
  }

  private static List<Backlog> getDeferralBacklogs() {
    return of(
        new Backlog(CPTS.get(0), 1000),
        new Backlog(CPTS.get(1), 1000),
        new Backlog(CPTS.get(2), 2000)
    );
  }

  private static EntityOutput buildEntityOutput(final ZonedDateTime date, final ProcessName process, final long value) {
    return EntityOutput.builder()
        .date(date)
        .processName(process)
        .type(THROUGHPUT)
        .metricUnit(UNITS_PER_HOUR)
        .source(FORECAST)
        .value(value)
        .build();
  }

  private static void assertDeferredResult(final DeferralProjectionOutput result,
                                           final ZonedDateTime sla,
                                           final Instant deferredAt,
                                           final int deferredUnits,
                                           final DeferralStatus deferralStatus) {

    assertEquals(sla.toInstant(), result.getSla());
    assertEquals(deferredAt, result.getDeferredAt());
    assertEquals(deferredUnits, result.getDeferredUnits());
    assertEquals(deferralStatus, result.getDeferralStatus());

  }

  private static GetDeferralProjectionInput getInput(final List<Backlog> backlogs) {
    final ZonedDateTime dateFrom = TRUNCATED_NOW;
    final ZonedDateTime dateTo = TRUNCATED_NOW.plusHours(8);
    final ZonedDateTime slaFrom = dateFrom;
    final ZonedDateTime slaTo = dateFrom.plus(72, HOURS);

    return new GetDeferralProjectionInput(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        ProjectionType.DEFERRAL,
        dateFrom.toInstant(),
        dateFrom,
        dateTo,
        slaFrom,
        slaTo,
        backlogs,
        null,
        true
    );
  }

  @BeforeEach
  public void setUp() {
    dateUtils = mockStatic(DateUtils.class);
    dateUtils.when(DateUtils::getCurrentUtcDate)
        .thenReturn(NOW.truncatedTo(SECONDS));
    dateUtils.when(() -> DateUtils.ignoreMinutes(any()))
        .thenCallRealMethod();

    when(getCapacityPerHourService.execute(any(), any())).thenCallRealMethod();
  }

  @AfterEach
  public void tearDown() {
    dateUtils.close();
  }

  @Test
  public void testProjectionWhenDeferralsAreNotNecessary() {
    //GIVEN
    final List<Backlog> backlogs = getNoDeferralBacklogs();
    final GetDeferralProjectionInput input = getInput(backlogs);

    mockSlas(input.getSlaFrom(), input.getSlaTo());
    mockCycleTimes();
    mockPlannedBacklog(input.getSlaFrom(), input.getSlaTo());
    mockThroughput(input.getDateFrom(), input.getDateTo());
    mockMaxCaps(input);

    //WHEN
    final var projectionResult = getDeliveryPromiseUseCase.execute(input);

    //THEN
    assertTrue(projectionResult.isEmpty());
  }

  @Test
  public void testProjectionWhenDeferralsAreNecessary() {
    //GIVEN
    final List<Backlog> backlogs = getDeferralBacklogs();
    final GetDeferralProjectionInput input = getInput(backlogs);

    mockSlas(input.getSlaFrom(), input.getSlaTo());
    mockCycleTimes();
    mockPlannedBacklog(input.getSlaFrom(), input.getSlaTo());
    mockThroughput(input.getDateFrom(), input.getDateTo());
    mockMaxCaps(input);

    //WHEN
    final var projectionResult = getDeliveryPromiseUseCase.execute(input);

    //THEN
    final Instant firstDeferralsAt = Instant.parse("2022-06-08T12:55:00Z");
    final Instant secondDeferralsAt = Instant.parse("2022-06-08T13:55:00Z");

    assertEquals(4, projectionResult.size());

    assertDeferredResult(projectionResult.get(0), CPTS.get(0), firstDeferralsAt, 200, DeferralStatus.DEFERRED_CASCADE);
    assertDeferredResult(projectionResult.get(1), CPTS.get(1), firstDeferralsAt, 400, DeferralStatus.DEFERRED_CASCADE);
    assertDeferredResult(projectionResult.get(2), CPTS.get(2), firstDeferralsAt, 600, DeferralStatus.DEFERRED_CAP_MAX);
    assertDeferredResult(projectionResult.get(3), CPTS.get(3), secondDeferralsAt, 108, DeferralStatus.DEFERRED_CAP_MAX);
  }

  @Test
  public void testProjectionWhenAllCptsAreCurrentlyDeferred() {
    //GIVEN
    final List<Backlog> backlogs = getDeferralBacklogs();
    final GetDeferralProjectionInput input = getInput(backlogs);

    Map<ZonedDateTime, Integer> maxCaps = new ConcurrentHashMap<>();
    OPERATING_HOURS.subList(0, 7).forEach(date -> maxCaps.put(date, 0));
    maxCaps.put(OPERATING_HOURS.get(7),100000);

    when(maxCapacityService.getMaxCapacity(
        new MaxCapacityInput(
            WAREHOUSE_ID,
            input.getWorkflow(),
            input.getDateFrom(),
            input.getDateTo(),
            Collections.emptyList()
        )
        )).thenReturn(maxCaps);

    mockSlas(input.getSlaFrom(), input.getSlaTo());
    mockCycleTimes();
    mockPlannedBacklog(input.getSlaFrom(), input.getSlaTo());
    mockThroughput(input.getDateFrom(), input.getDateTo());

    //WHEN
    final var projectionResult = getDeliveryPromiseUseCase.execute(input);

    //THEN
    final Instant firstDeferralsAt = Instant.parse("2022-06-08T12:00:00Z");
    final Instant secondDeferralsAt = Instant.parse("2022-06-08T13:05:00Z");

    assertEquals(4, projectionResult.size());

    assertDeferredResult(projectionResult.get(0), CPTS.get(0), firstDeferralsAt, 200, DeferralStatus.DEFERRED_CAP_MAX);
    assertDeferredResult(projectionResult.get(1), CPTS.get(1), firstDeferralsAt, 400, DeferralStatus.DEFERRED_CAP_MAX);
    assertDeferredResult(projectionResult.get(2), CPTS.get(2), firstDeferralsAt, 600, DeferralStatus.DEFERRED_CAP_MAX);
    assertDeferredResult(projectionResult.get(3), CPTS.get(3), secondDeferralsAt, 191, DeferralStatus.DEFERRED_CAP_MAX);
  }

  private void mockPlannedBacklog(final ZonedDateTime dateFrom, final ZonedDateTime dateTo) {
    final var forecastByCpt = Map.of(
        CPTS.get(0), 100,
        CPTS.get(1), 200,
        CPTS.get(2), 300,
        CPTS.get(3), 100
    );

    final List<PlannedUnits> plannedUnits = OPERATING_HOURS
        .subList(1, 3)
        .stream()
        .flatMap(oh -> forecastByCpt.entrySet()
            .stream()
            .map(entry -> new PlannedUnits(oh, entry.getKey(), entry.getValue()))
        ).collect(Collectors.toList());

    when(
        plannedBacklogService.getExpectedBacklog(
            WAREHOUSE_ID,
            FBM_WMS_OUTBOUND,
            dateFrom,
            dateTo,
            true
        )
    ).thenReturn(plannedUnits);
  }

  private void mockThroughput(final ZonedDateTime dateFrom, final ZonedDateTime dateTo) {
    final List<EntityOutput> throughput = Stream.of(
            OPERATING_HOURS.stream()
                .map(date -> buildEntityOutput(date, PACKING, 300)),
            OPERATING_HOURS.stream()
                .map(date -> buildEntityOutput(date, PACKING_WALL, 300))
        ).flatMap(Function.identity())
        .collect(Collectors.toList());

    when(
        getThroughputUseCase.execute(GetEntityInput
            .builder()
            .warehouseId(WAREHOUSE_ID)
            .workflow(FBM_WMS_OUTBOUND)
            .dateFrom(dateFrom)
            .dateTo(dateTo)
            .processName(of(PACKING, PACKING_WALL))
            .source(Source.SIMULATION)
            .build())
    ).thenReturn(throughput);
  }

  private void mockMaxCaps(GetDeferralProjectionInput input) {
    Map<ZonedDateTime, Integer> maxCaps = new ConcurrentHashMap<>();
    OPERATING_HOURS.forEach(date -> maxCaps.put(date, 750));

    when(maxCapacityService.getMaxCapacity(
        new MaxCapacityInput(
            WAREHOUSE_ID,
            input.getWorkflow(),
            input.getDateFrom(),
            input.getDateTo(),
            Collections.emptyList()
        )
    )).thenReturn(maxCaps);

  }
  private void mockSlas(final ZonedDateTime dateFrom, final ZonedDateTime dateTo) {
    final Function<ZonedDateTime, GetSlaByWarehouseOutput> buildSla = sla ->
        GetSlaByWarehouseOutput.builder().date(sla).processingTime(new ProcessingTime(60L, MINUTES)).build();

    final List<GetSlaByWarehouseOutput> slas = CPTS.stream()
        .map(buildSla)
        .collect(Collectors.toList());

    when(getSlaByWarehouseOutboundService.execute(
        new GetSlaByWarehouseInput(
            WAREHOUSE_ID,
            dateFrom,
            dateTo,
            of(CPTS.get(0), CPTS.get(1), CPTS.get(2)),
            null))
    ).thenReturn(slas);
  }

  private void mockCycleTimes() {
    final Map<ZonedDateTime, Long> cycleTimeByCpt = CPTS.stream()
        .collect(Collectors.toMap(Function.identity(), d -> 30L));

    when(getCycleTimeService.execute(
        new GetCycleTimeInput(
            WAREHOUSE_ID,
            CPTS
        )
    )).thenReturn(cycleTimeByCpt);
  }
}
