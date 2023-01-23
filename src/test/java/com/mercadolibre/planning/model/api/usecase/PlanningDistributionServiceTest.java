package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper.DATE_IN;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper.DATE_OUT;
import static com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper.PROCESS_PATH;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.currentPlanningDistributions;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastIds;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDistributionInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.planningDistributions;
import static java.util.Set.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.current.CurrentPlanningDistributionRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.client.db.repository.forecast.PlanningDistributionRepository;
import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.current.CurrentPlanningDistribution;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.Grouper;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionOutput.GroupKey;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService.PlannedUnitsGateway;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService.PlanningDistributionInput;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.LongVariable")
public class PlanningDistributionServiceTest {

  private static final Instant DATE_IN_1 = A_DATE_UTC.toInstant();

  private static final Instant DATE_IN_2 = A_DATE_UTC.plusHours(1).toInstant();

  private static final Instant DATE_IN_3 = A_DATE_UTC.plusHours(2).toInstant();

  private static final Instant DATE_OUT_1 = A_DATE_UTC.plusHours(6).toInstant();

  private static final Instant DATE_OUT_2 = A_DATE_UTC.plusHours(7).toInstant();

  private static final Instant DATE_OUT_3 = A_DATE_UTC.plusHours(8).toInstant();

  @Mock
  private PlanningDistributionRepository planningDistRepository;

  @Mock
  private CurrentPlanningDistributionRepository currentPlanningDistRepository;

  @Mock
  private GetForecastUseCase getForecastUseCase;

  @Mock
  private CurrentForecastDeviationRepository currentForecastDeviationRepository;

  @Mock
  private PlannedUnitsGateway repository;

  @InjectMocks
  private PlanningDistributionService planningDistributionService;

  private static PlanningDistributionInput input(final boolean applyDeviation, final Set<Grouper> groupers) {
    return new PlanningDistributionInput(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        of(TOT_MONO, NON_TOT_MONO),
        DATE_IN_1,
        DATE_IN_3,
        DATE_OUT_1,
        DATE_OUT_3,
        groupers,
        applyDeviation,
        A_DATE_UTC.toInstant()
    );
  }

  @Test
  @DisplayName("Get planning distribution from forecast without date in to")
  public void testGetPlanningDistributionOk() {
    // GIVEN
    final GetPlanningDistributionInput input = mockPlanningDistributionInput(null, null);

    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateOutFrom())
        .dateTo(input.getDateOutTo())
        .build())
    ).thenReturn(mockForecastIds());

    when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        A_DATE_UTC,
        A_DATE_UTC.plusDays(3),
        null,
        null,
        mockForecastIds())
    ).thenReturn(planningDistributions());

    // WHEN
    final List<GetPlanningDistributionOutput> output = planningDistributionService
        .getPlanningDistribution(input);

    // THEN
    final GetPlanningDistributionOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.toInstant(), output1.getDateIn().toInstant());
    assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
    assertEquals(1000, output1.getTotal());
    assertEquals(UNITS, output1.getMetricUnit());

    final GetPlanningDistributionOutput output3 = output.get(2);
    assertEquals(A_DATE_UTC.toInstant(), output3.getDateIn().toInstant());
    assertEquals(A_DATE_UTC.plusDays(2).toInstant(), output3.getDateOut().toInstant());
    assertEquals(1200, output3.getTotal());
    assertEquals(UNITS, output3.getMetricUnit());
  }

  @Test
  @DisplayName("Get planning distribution from forecast with date in to")
  public void testGetPlanningDistributionWithDateInToOk() {
    // GIVEN
    final ZonedDateTime dateInTo = A_DATE_UTC.minusDays(3);
    final GetPlanningDistributionInput input = mockPlanningDistributionInput(null, dateInTo);

    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateOutFrom())
        .dateTo(input.getDateOutTo())
        .build())
    ).thenReturn(mockForecastIds());

    when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        A_DATE_UTC,
        A_DATE_UTC.plusDays(3),
        null,
        dateInTo,
        mockForecastIds())
    ).thenReturn(planningDistributions());

    // WHEN
    final List<GetPlanningDistributionOutput> output = planningDistributionService
        .getPlanningDistribution(input);

    // THEN
    final GetPlanningDistributionOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.toInstant(), output1.getDateIn().toInstant());
    assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
    assertEquals(1000, output1.getTotal());
    assertEquals(UNITS, output1.getMetricUnit());

    final GetPlanningDistributionOutput output2 = output.get(1);
    assertEquals(A_DATE_UTC.toInstant(), output2.getDateIn().toInstant());
    assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output2.getDateOut().toInstant());
    assertEquals(300, output2.getTotal());
    assertEquals(UNITS, output2.getMetricUnit());

    final GetPlanningDistributionOutput output3 = output.get(2);
    assertEquals(A_DATE_UTC.toInstant(), output3.getDateIn().toInstant());
    assertEquals(A_DATE_UTC.plusDays(2).toInstant(), output3.getDateOut().toInstant());
    assertEquals(1200, output3.getTotal());
    assertEquals(UNITS, output3.getMetricUnit());
  }

  @Test
  @DisplayName("Get planning distribution from forecast wih date in from and date in to")
  public void testGetPlanningDistributionWithDateInFromAndDateInToOk() {
    // GIVEN
    final ZonedDateTime dateInFrom = A_DATE_UTC.minusDays(3);
    final GetPlanningDistributionInput input = mockPlanningDistributionInput(
        dateInFrom, A_DATE_UTC);

    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateOutFrom())
        .dateTo(input.getDateOutTo())
        .build())
    ).thenReturn(mockForecastIds());

    when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        A_DATE_UTC,
        A_DATE_UTC.plusDays(3),
        dateInFrom,
        A_DATE_UTC,
        mockForecastIds())
    ).thenReturn(planningDistributions());

    // WHEN
    final List<GetPlanningDistributionOutput> output = planningDistributionService
        .getPlanningDistribution(input);

    // THEN
    final GetPlanningDistributionOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.toInstant(), output1.getDateIn().toInstant());
    assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
    assertEquals(1000, output1.getTotal());
    assertEquals(UNITS, output1.getMetricUnit());

    final GetPlanningDistributionOutput output3 = output.get(2);
    assertEquals(A_DATE_UTC.toInstant(), output3.getDateIn().toInstant());
    assertEquals(A_DATE_UTC.plusDays(2).toInstant(), output3.getDateOut().toInstant());
    assertEquals(1200, output3.getTotal());
    assertEquals(UNITS, output3.getMetricUnit());
  }

  @Test
  @DisplayName("Get planning distribution applying current planning distribution")
  public void testGetPlanningDistributionApplyingCurrentPlanningDistribution() {
    // GIVEN
    final GetPlanningDistributionInput input = mockPlanningDistributionInput(null, null);

    final List<CurrentPlanningDistribution> distributions = currentPlanningDistributions();

    when(currentPlanningDistRepository
        .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
            FBM_WMS_OUTBOUND,
            WAREHOUSE_ID,
            A_DATE_UTC,
            A_DATE_UTC.plusDays(3)
        )).thenReturn(distributions);

    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateOutFrom())
        .dateTo(input.getDateOutTo())
        .build())
    ).thenReturn(mockForecastIds());

    when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        A_DATE_UTC,
        A_DATE_UTC.plusDays(3),
        null,
        null,
        mockForecastIds())
    ).thenReturn(planningDistributions());

    // WHEN
    final List<GetPlanningDistributionOutput> output = planningDistributionService
        .getPlanningDistribution(input);

    // THEN
    final GetPlanningDistributionOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
    assertEquals(1000, output1.getTotal());
    assertFalse(output1.isDeferred());

    final List<GetPlanningDistributionOutput> recordsForSecondDay =
        output.stream()
            .filter(item -> item.getDateOut()
                .toInstant()
                .equals(A_DATE_UTC.plusDays(2).toInstant()))
            .collect(Collectors.toUnmodifiableList());

    final Long outputTotalForSecondDay = recordsForSecondDay.stream()
        .map(GetPlanningDistributionOutput::getTotal)
        .reduce(0L, Long::sum);

    assertEquals(2, recordsForSecondDay.size());
    assertEquals(Long.valueOf(3700), outputTotalForSecondDay);
    assertTrue(recordsForSecondDay.stream()
        .allMatch(GetPlanningDistributionOutput::isDeferred)
    );
  }

  @Test
  @DisplayName("Get planning distribution applying both: current planning distribution and forecast deviation")
  public void testGetPlanningDistributionApplyingCurrentPlanningDistributionAndForecastDeviation() {
    // GIVEN
    final GetPlanningDistributionInput input = GetPlanningDistributionInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .dateOutFrom(A_DATE_UTC)
        .dateOutTo(A_DATE_UTC.plusDays(3))
        .applyDeviation(true)
        .build();


    final List<CurrentPlanningDistribution> distributions = currentPlanningDistributions();

    when(currentPlanningDistRepository
        .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
            FBM_WMS_OUTBOUND,
            WAREHOUSE_ID,
            A_DATE_UTC,
            A_DATE_UTC.plusDays(3)
        )).thenReturn(distributions);

    when(getForecastUseCase.execute(GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(input.getDateOutFrom())
        .dateTo(input.getDateOutTo())
        .build())
    ).thenReturn(mockForecastIds());

    when(planningDistRepository.findByWarehouseIdWorkflowAndDateOutAndDateInInRange(
        A_DATE_UTC,
        A_DATE_UTC.plusDays(3),
        null,
        null,
        mockForecastIds())
    ).thenReturn(planningDistributions());

    when(currentForecastDeviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowIn(WAREHOUSE_ID, of(FBM_WMS_OUTBOUND)))
        .thenReturn(
            List.of(new CurrentForecastDeviation(
                1, input.getWarehouseId(), A_DATE_UTC, A_DATE_UTC, 1.0, true, 123L,
                FBM_WMS_OUTBOUND, A_DATE_UTC, A_DATE_UTC, DeviationType.UNITS, null
            ))
        );

    // WHEN
    final List<GetPlanningDistributionOutput> output = planningDistributionService
        .getPlanningDistribution(input);

    // THEN
    final GetPlanningDistributionOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut().toInstant());
    assertEquals(2000, output1.getTotal());
    assertFalse(output1.isDeferred());

    final List<GetPlanningDistributionOutput> recordsForSecondDay =
        output.stream()
            .filter(item -> item.getDateOut()
                .toInstant()
                .equals(A_DATE_UTC.plusDays(2).toInstant()))
            .collect(Collectors.toUnmodifiableList());

    final Long outputTotalForSecondDay = recordsForSecondDay.stream()
        .map(GetPlanningDistributionOutput::getTotal)
        .reduce(0L, Long::sum);

    assertEquals(2, recordsForSecondDay.size());
    assertEquals(Long.valueOf(4900), outputTotalForSecondDay);
    assertTrue(recordsForSecondDay.stream()
        .allMatch(GetPlanningDistributionOutput::isDeferred)
    );
  }

  @Test
  @DisplayName("Get planning distribution duplicate key")
  public void testGetPlanningDistributionDuplicateKey() {

    try {
      // GIVEN
      final GetPlanningDistributionInput input = mockPlanningDistributionInput(null, null);

      final CurrentPlanningDistribution first = mock(CurrentPlanningDistribution.class);
      final CurrentPlanningDistribution second = mock(CurrentPlanningDistribution.class);

      final List<CurrentPlanningDistribution> currentPlanningDistributions =
          List.of(first, second);

      when(first.getDateOut()).thenReturn(A_DATE_UTC);
      when(second.getDateOut()).thenReturn(A_DATE_UTC);

      when(currentPlanningDistRepository
          .findByWorkflowAndLogisticCenterIdAndDateOutBetweenAndIsActiveTrue(
              FBM_WMS_OUTBOUND,
              WAREHOUSE_ID,
              A_DATE_UTC,
              A_DATE_UTC.plusDays(3)
          )).thenReturn(currentPlanningDistributions);

      // WHEN
      planningDistributionService.getPlanningDistribution(input);

    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().contains("Duplicate key"));
    }
  }

  @Test
  @DisplayName("Get planning distribution by Process Path with deviations")
  void testGetPlanningDistributionByProcessPathWithDeviations() {
    // GIVEN
    final var groupers = of(DATE_IN, DATE_OUT, PROCESS_PATH);

    final var input = input(true, groupers);

    final var distributions = List.of(
        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, TOT_MONO, 33.5),
        new PlanningDistribution(1L, DATE_IN_2, DATE_OUT_3, TOT_MONO, 44.5),
        new PlanningDistribution(1L, DATE_IN_3, DATE_OUT_2, TOT_MONO, 30.0),

        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, NON_TOT_MONO, 0),
        new PlanningDistribution(1L, DATE_IN_3, DATE_OUT_2, NON_TOT_MONO, 10)
    );

    when(repository.getPlanningDistributions(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        of(TOT_MONO, NON_TOT_MONO),
        DATE_IN_1,
        DATE_IN_3,
        DATE_OUT_1,
        DATE_OUT_3,
        groupers,
        A_DATE_UTC.toInstant()
    )).thenReturn(distributions);

    when(currentForecastDeviationRepository.findActiveDeviationAt(WAREHOUSE_ID, FBM_WMS_OUTBOUND.name(), input.getViewDate()))
        .thenReturn(List.of(CurrentForecastDeviation.builder()
            .dateFrom(ZonedDateTime.ofInstant(DATE_IN_2, ZoneOffset.UTC))
            .dateTo(ZonedDateTime.ofInstant(DATE_IN_3, ZoneOffset.UTC))
            .value(0.5)
            .build())
        );

    // WHEN
    final var actual = planningDistributionService.getPlanningDistribution(input);

    // THEN
    assertNotNull(actual);

    final var expected = List.of(
        new PlanningDistributionOutput(new GroupKey(TOT_MONO, DATE_IN_1, DATE_OUT_1), 33.5),
        new PlanningDistributionOutput(new GroupKey(TOT_MONO, DATE_IN_2, DATE_OUT_3), 66.75),
        new PlanningDistributionOutput(new GroupKey(TOT_MONO, DATE_IN_3, DATE_OUT_2), 45.0),
        new PlanningDistributionOutput(new GroupKey(NON_TOT_MONO, DATE_IN_1, DATE_OUT_1), 0),
        new PlanningDistributionOutput(new GroupKey(NON_TOT_MONO, DATE_IN_3, DATE_OUT_2), 15)
    );

    assertEquals(expected, actual);

    verifyNoInteractions(getForecastUseCase);
    verifyNoInteractions(planningDistRepository);
    verifyNoInteractions(currentPlanningDistRepository);
  }

  @Test
  @DisplayName("Get planning distribution by Process Path without deviations")
  void testGetPlanningDistributionByProcessPathWithoutDeviations() {
    // GIVEN
    final var groupers = of(DATE_IN, DATE_OUT, PROCESS_PATH);

    final var input = input(false, groupers);

    final var distributions = List.of(
        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, TOT_MONO, 33.5),
        new PlanningDistribution(1L, DATE_IN_2, DATE_OUT_3, TOT_MONO, 44.5),
        new PlanningDistribution(1L, DATE_IN_3, DATE_OUT_2, TOT_MONO, 30.0),

        new PlanningDistribution(1L, DATE_IN_1, DATE_OUT_1, NON_TOT_MONO, 0),
        new PlanningDistribution(1L, DATE_IN_3, DATE_OUT_2, NON_TOT_MONO, 10)
    );

    when(repository.getPlanningDistributions(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        of(TOT_MONO, NON_TOT_MONO),
        DATE_IN_1,
        DATE_IN_3,
        DATE_OUT_1,
        DATE_OUT_3,
        groupers,
        A_DATE_UTC.toInstant()
    )).thenReturn(distributions);

    // WHEN
    final var actual = planningDistributionService.getPlanningDistribution(input);

    // THEN
    assertNotNull(actual);

    final var expected = List.of(
        new PlanningDistributionOutput(new GroupKey(TOT_MONO, DATE_IN_1, DATE_OUT_1), 33.5),
        new PlanningDistributionOutput(new GroupKey(TOT_MONO, DATE_IN_2, DATE_OUT_3), 44.5),
        new PlanningDistributionOutput(new GroupKey(TOT_MONO, DATE_IN_3, DATE_OUT_2), 30.0),
        new PlanningDistributionOutput(new GroupKey(NON_TOT_MONO, DATE_IN_1, DATE_OUT_1), 0),
        new PlanningDistributionOutput(new GroupKey(NON_TOT_MONO, DATE_IN_3, DATE_OUT_2), 10)
    );

    assertEquals(expected, actual);

    verifyNoInteractions(getForecastUseCase);
    verifyNoInteractions(planningDistRepository);
    verifyNoInteractions(currentPlanningDistRepository);
    verifyNoInteractions(currentForecastDeviationRepository);
  }

  @Test
  @DisplayName("Get planning distribution only by Process Path")
  void testGetPlanningDistributionByProcessPathWithASubsetOfGroupers() {
    // GIVEN
    final var groupers = of(PROCESS_PATH);

    final var input = input(false, groupers);

    final var distributions = List.of(
        new PlanningDistribution(1L, null, null, TOT_MONO, 810),
        new PlanningDistribution(1L, null, null, NON_TOT_MONO, 10)
    );

    when(repository.getPlanningDistributions(
        WAREHOUSE_ID,
        FBM_WMS_OUTBOUND,
        of(TOT_MONO, NON_TOT_MONO),
        DATE_IN_1,
        DATE_IN_3,
        DATE_OUT_1,
        DATE_OUT_3,
        groupers,
        A_DATE_UTC.toInstant()
    )).thenReturn(distributions);

    // WHEN
    final var actual = planningDistributionService.getPlanningDistribution(input);

    // THEN
    assertNotNull(actual);

    final var expected = List.of(
        new PlanningDistributionOutput(new GroupKey(TOT_MONO, null, null), 810),
        new PlanningDistributionOutput(new GroupKey(NON_TOT_MONO, null, null), 10)
    );

    assertEquals(expected, actual);

    verifyNoInteractions(getForecastUseCase);
    verifyNoInteractions(planningDistRepository);
    verifyNoInteractions(currentPlanningDistRepository);
    verifyNoInteractions(currentForecastDeviationRepository);
  }

}
