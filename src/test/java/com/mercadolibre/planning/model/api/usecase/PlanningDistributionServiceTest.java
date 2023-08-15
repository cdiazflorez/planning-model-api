package com.mercadolibre.planning.model.api.usecase;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.NON_TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MONO;
import static com.mercadolibre.planning.model.api.domain.entity.ProcessPath.TOT_MULTI_BATCH;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.A_DATE_UTC;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockForecastIds;
import static com.mercadolibre.planning.model.api.util.TestUtils.mockPlanningDistributionInput;
import static com.mercadolibre.planning.model.api.util.TestUtils.planningDistributions;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.Collections.emptySet;
import static java.util.Set.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.DeviationType;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastInput;
import com.mercadolibre.planning.model.api.domain.usecase.forecast.get.GetForecastUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.DeferralGateway;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanDistribution;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionGateway;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
  private GetForecastUseCase getForecastUseCase;

  @Mock
  private PlanningDistributionGateway planningDistributionGateway;

  @Mock
  private DeferralGateway deferralGateway;

  @Mock
  private CurrentForecastDeviationRepository currentForecastDeviationRepository;

  @InjectMocks
  private PlanningDistributionService planningDistributionService;

  @Test
  @DisplayName("Get planning distribution from forecast without date in to")
  public void testGetPlanningDistributionOk() {
    // GIVEN
    final GetPlanningDistributionInput input = mockPlanningDistributionInput(null,
                                                                             null,
                                                                             A_DATE_UTC.toInstant(),
                                                                             A_DATE_UTC.plusDays(3).toInstant(),
                                                                             null,
                                                                             false,
                                                                             emptySet(),
                                                                             false);

    when(getForecastUseCase.execute(mockForecastInput(input))).thenReturn(mockForecastIds());

    when(planningDistributionGateway.findByForecastIdsAndDynamicFilters(
        null,
        null,
        A_DATE_UTC.toInstant(),
        A_DATE_UTC.plusDays(3).toInstant(),
        of(),
        new HashSet<>(mockForecastIds()))
    ).thenReturn(planningDistributions());

    // WHEN
    final List<GetPlanningDistributionOutput> output = planningDistributionService
        .getPlanningDistribution(input);

    // THEN
    final GetPlanningDistributionOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.toInstant(), output1.getDateIn());
    assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut());
    assertEquals(1000, output1.getTotal());
    assertEquals(UNITS, output1.getMetricUnit());

    final GetPlanningDistributionOutput output3 = output.get(2);
    assertEquals(A_DATE_UTC.toInstant(), output3.getDateIn());
    assertEquals(A_DATE_UTC.plusDays(2).toInstant(), output3.getDateOut());
    assertEquals(1200, output3.getTotal());
    assertEquals(UNITS, output3.getMetricUnit());
  }

  @Test
  @DisplayName("Get planning distribution from forecast with date in to")
  public void testGetPlanningDistributionWithDateInToOk() {
    // GIVEN
    final Instant dateInTo = A_DATE_UTC.minusDays(3).toInstant();
    final GetPlanningDistributionInput input = mockPlanningDistributionInput(null,
                                                                             dateInTo,
                                                                             A_DATE_UTC.toInstant(),
                                                                             A_DATE_UTC.plusDays(3).toInstant(),
                                                                             null,
                                                                             false,
                                                                             emptySet(),
                                                                             false);

    when(getForecastUseCase.execute(mockForecastInput(input))).thenReturn(mockForecastIds());

    when(planningDistributionGateway.findByForecastIdsAndDynamicFilters(
        null,
        dateInTo,
        A_DATE_UTC.toInstant(),
        A_DATE_UTC.plusDays(3).toInstant(),
        of(),
        new HashSet<>(mockForecastIds()))
    ).thenReturn(planningDistributions());

    // WHEN
    final List<GetPlanningDistributionOutput> output = planningDistributionService
        .getPlanningDistribution(input);

    // THEN
    final GetPlanningDistributionOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.toInstant(), output1.getDateIn());
    assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut());
    assertEquals(1000, output1.getTotal());
    assertEquals(UNITS, output1.getMetricUnit());

    final GetPlanningDistributionOutput output2 = output.get(1);
    assertEquals(A_DATE_UTC.toInstant(), output2.getDateIn());
    assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output2.getDateOut());
    assertEquals(300, output2.getTotal());
    assertEquals(UNITS, output2.getMetricUnit());

    final GetPlanningDistributionOutput output3 = output.get(2);
    assertEquals(A_DATE_UTC.toInstant(), output3.getDateIn());
    assertEquals(A_DATE_UTC.plusDays(2).toInstant(), output3.getDateOut());
    assertEquals(1200, output3.getTotal());
    assertEquals(UNITS, output3.getMetricUnit());
  }

  @Test
  @DisplayName("Get planning distribution from forecast wih date in from and date in to")
  public void testGetPlanningDistributionWithDateInFromAndDateInToOk() {
    // GIVEN
    final Instant dateInFrom = A_DATE_UTC.minusDays(3).toInstant();
    final GetPlanningDistributionInput input = mockPlanningDistributionInput(dateInFrom,
                                                                             A_DATE_UTC.toInstant(),
                                                                             A_DATE_UTC.toInstant(),
                                                                             A_DATE_UTC.plusDays(3).toInstant(),
                                                                             null,
                                                                             false,
                                                                             emptySet(),
                                                                             false);

    when(getForecastUseCase.execute(mockForecastInput(input))).thenReturn(mockForecastIds());

    when(planningDistributionGateway.findByForecastIdsAndDynamicFilters(
        dateInFrom,
        A_DATE_UTC.toInstant(),
        A_DATE_UTC.toInstant(),
        A_DATE_UTC.plusDays(3).toInstant(),
        of(),
        new HashSet<>(mockForecastIds()))
    ).thenReturn(planningDistributions());

    // WHEN
    final List<GetPlanningDistributionOutput> output = planningDistributionService
        .getPlanningDistribution(input);

    // THEN
    final GetPlanningDistributionOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.toInstant(), output1.getDateIn());
    assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut());
    assertEquals(1000, output1.getTotal());
    assertEquals(UNITS, output1.getMetricUnit());

    final GetPlanningDistributionOutput output3 = output.get(2);
    assertEquals(A_DATE_UTC.toInstant(), output3.getDateIn());
    assertEquals(A_DATE_UTC.plusDays(2).toInstant(), output3.getDateOut());
    assertEquals(1200, output3.getTotal());
    assertEquals(UNITS, output3.getMetricUnit());
  }

  @Test
  @DisplayName("Get planning distribution applying forecast deviation")
  public void testGetPlanningDistributionApplyingCurrentPlanningDistributionAndForecastDeviation() {
    // GIVEN
    final GetPlanningDistributionInput input = GetPlanningDistributionInput.builder()
        .warehouseId(WAREHOUSE_ID)
        .workflow(FBM_WMS_OUTBOUND)
        .dateOutFrom(A_DATE_UTC.toInstant())
        .dateOutTo(A_DATE_UTC.plusDays(3).toInstant())
        .applyDeviation(true)
        .applyDeferrals(false)
        .build();

    when(getForecastUseCase.execute(mockForecastInput(input))).thenReturn(mockForecastIds());

    when(planningDistributionGateway.findByForecastIdsAndDynamicFilters(
        null,
        null,
        A_DATE_UTC.toInstant(),
        A_DATE_UTC.plusDays(3).toInstant(),
        of(),
        new HashSet<>(mockForecastIds()))
    ).thenReturn(planningDistributions());

    when(currentForecastDeviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowIn(WAREHOUSE_ID, of(FBM_WMS_OUTBOUND)))
        .thenReturn(
            List.of(new CurrentForecastDeviation(
                1,
                input.getWarehouseId(),
                A_DATE_UTC,
                A_DATE_UTC,
                1.0,
                true,
                123L,
                FBM_WMS_OUTBOUND,
                A_DATE_UTC,
                A_DATE_UTC,
                DeviationType.UNITS,
                null
            ))
        );

    // WHEN
    final List<GetPlanningDistributionOutput> output = planningDistributionService
        .getPlanningDistribution(input);

    // THEN
    final GetPlanningDistributionOutput output1 = output.get(0);
    assertEquals(A_DATE_UTC.plusDays(1).toInstant(), output1.getDateOut());
    assertEquals(2000, output1.getTotal());

    final List<GetPlanningDistributionOutput> recordsForSecondDay =
        output.stream()
            .filter(item -> item.getDateOut()
                .equals(A_DATE_UTC.plusDays(2).toInstant()))
            .collect(Collectors.toUnmodifiableList());

    final Double outputTotalForSecondDay = recordsForSecondDay.stream()
        .map(GetPlanningDistributionOutput::getTotal)
        .reduce(0D, Double::sum);

    assertEquals(2, recordsForSecondDay.size());
    assertEquals(Double.valueOf(3650), outputTotalForSecondDay);
  }

  @Test
  @DisplayName("Get planning distribution by Process Path with deviations")
  void testGetPlanningDistributionByProcessPathWithDeviations() {
    // GIVEN
    final var processPaths = of(TOT_MONO, NON_TOT_MONO);

    final var input = mockPlanningDistributionInput(DATE_IN_1,
                                                    DATE_IN_3,
                                                    DATE_OUT_1,
                                                    DATE_OUT_3,
                                                    A_DATE_UTC.toInstant(),
                                                    true,
                                                    processPaths,
                                                    false);

    when(getForecastUseCase.execute(mockForecastInput(input))).thenReturn(mockForecastIds());

    final var distributions = List.of(
        new PlanDistribution(1L, DATE_IN_1, DATE_OUT_1, TOT_MONO, UNITS, 33.5),
        new PlanDistribution(1L, DATE_IN_2, DATE_OUT_3, TOT_MONO, UNITS, 44.5),
        new PlanDistribution(1L, DATE_IN_3, DATE_OUT_2, TOT_MONO, UNITS, 30.0),

        new PlanDistribution(1L, DATE_IN_1, DATE_OUT_1, NON_TOT_MONO, UNITS, 0),
        new PlanDistribution(1L, DATE_IN_3, DATE_OUT_2, NON_TOT_MONO, UNITS, 10)
    );

    when(planningDistributionGateway.findByForecastIdsAndDynamicFilters(
        DATE_IN_1,
        DATE_IN_3,
        DATE_OUT_1,
        DATE_OUT_3,
        processPaths,
        new HashSet<>(mockForecastIds())
    )).thenReturn(distributions);

    when(currentForecastDeviationRepository.findActiveDeviationAt(WAREHOUSE_ID, FBM_WMS_OUTBOUND.name(), input.getViewDate()))
        .thenReturn(List.of(CurrentForecastDeviation.builder()
                                .dateFrom(ofInstant(DATE_IN_2, UTC))
                                .dateTo(ofInstant(DATE_IN_3, UTC))
                                .value(0.5)
                                .build())
        );

    // WHEN
    final var actual = planningDistributionService.getPlanningDistribution(input);

    // THEN
    assertNotNull(actual);

    final var expected = List.of(
        new GetPlanningDistributionOutput(DATE_IN_1, DATE_OUT_1, UNITS, TOT_MONO, 33.5),
        new GetPlanningDistributionOutput(DATE_IN_2, DATE_OUT_3, UNITS, TOT_MONO, 66.75),
        new GetPlanningDistributionOutput(DATE_IN_3, DATE_OUT_2, UNITS, TOT_MONO, 45.0),
        new GetPlanningDistributionOutput(DATE_IN_1, DATE_OUT_1, UNITS, NON_TOT_MONO, 0),
        new GetPlanningDistributionOutput(DATE_IN_3, DATE_OUT_2, UNITS, NON_TOT_MONO, 15)
    );

    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual) && actual.containsAll(expected));
  }

  @Test
  @DisplayName("Get planning distribution by Process Path without deviations")
  void testGetPlanningDistributionByProcessPathWithoutDeviations() {
    // GIVEN
    final var processPaths = of(TOT_MONO, NON_TOT_MONO);

    final var input = mockPlanningDistributionInput(DATE_IN_1,
                                                    DATE_IN_3,
                                                    null,
                                                    null,
                                                    A_DATE_UTC.toInstant(),
                                                    false,
                                                    processPaths,
                                                    false);

    when(getForecastUseCase.execute(mockForecastInput(input))).thenReturn(mockForecastIds());

    final var distributions = List.of(
        new PlanDistribution(1L, DATE_IN_1, DATE_OUT_1, TOT_MONO, UNITS, 33.5),
        new PlanDistribution(1L, DATE_IN_2, DATE_OUT_3, TOT_MONO, UNITS, 44.5),
        new PlanDistribution(1L, DATE_IN_3, DATE_OUT_2, TOT_MONO, UNITS, 30.0),

        new PlanDistribution(1L, DATE_IN_1, DATE_OUT_1, NON_TOT_MONO, UNITS, 0),
        new PlanDistribution(1L, DATE_IN_3, DATE_OUT_2, NON_TOT_MONO, UNITS, 10)
    );

    when(planningDistributionGateway.findByForecastIdsAndDynamicFilters(
        DATE_IN_1,
        DATE_IN_3,
        null,
        null,
        processPaths,
        new HashSet<>(mockForecastIds())
    )).thenReturn(distributions);

    // WHEN
    final var actual = planningDistributionService.getPlanningDistribution(input);

    // THEN
    assertNotNull(actual);

    final var expected = List.of(
        new GetPlanningDistributionOutput(DATE_IN_1, DATE_OUT_1, UNITS, TOT_MONO, 33.5),
        new GetPlanningDistributionOutput(DATE_IN_2, DATE_OUT_3, UNITS, TOT_MONO, 44.5),
        new GetPlanningDistributionOutput(DATE_IN_3, DATE_OUT_2, UNITS, TOT_MONO, 30.0),
        new GetPlanningDistributionOutput(DATE_IN_1, DATE_OUT_1, UNITS, NON_TOT_MONO, 0),
        new GetPlanningDistributionOutput(DATE_IN_3, DATE_OUT_2, UNITS, NON_TOT_MONO, 10)
    );

    assertEquals(expected.size(), actual.size());
    assertTrue(expected.containsAll(actual) && actual.containsAll(expected));
    verifyNoInteractions(currentForecastDeviationRepository);
    verifyNoInteractions(deferralGateway);
  }

    @ParameterizedTest
    @MethodSource("forecastArguments")
    @DisplayName("forecasted backlog get with request flag is apply deferred")
    void testRequestFlagIsApplyDeferred(
            final GetPlanningDistributionInput input,
            final List<GetPlanningDistributionOutput> expected
    ) {
        // GIVEN
        final var distributions = List.of(
                new PlanDistribution(1L, DATE_IN_1, DATE_OUT_1, TOT_MONO, UNITS, 33.5),
                new PlanDistribution(1L, DATE_IN_2, DATE_OUT_3, TOT_MONO, UNITS, 44.5),
                new PlanDistribution(1L, DATE_IN_3, DATE_OUT_2, TOT_MONO, UNITS, 30.0),
                new PlanDistribution(1L, DATE_IN_1, DATE_OUT_1, NON_TOT_MONO, UNITS, 0),
                new PlanDistribution(1L, DATE_IN_3, DATE_OUT_2, NON_TOT_MONO, UNITS, 10)
        );

        when(getForecastUseCase.execute(mockForecastInput(input))).thenReturn(mockForecastIds());

        when(planningDistributionGateway.findByForecastIdsAndDynamicFilters(
                DATE_IN_1,
                DATE_IN_3,
                DATE_OUT_1,
                DATE_OUT_3,
                of(TOT_MONO, TOT_MULTI_BATCH),
                new HashSet<>(mockForecastIds())
        )).thenReturn(distributions);

        when(deferralGateway.getDeferredCpts(
                input.getWarehouseId(),
                input.getWorkflow(),
                input.getViewDate()
        )).thenReturn(List.of(DATE_OUT_1, DATE_OUT_2));

        // WHEN
        final List<GetPlanningDistributionOutput> output = planningDistributionService
                .getPlanningDistribution(input);

        // THEN
        assertNotNull(output);
        assertTrue(expected.containsAll(output) && output.containsAll(expected));
    }

    private static Stream<Arguments> forecastArguments() {
        return Stream.of(
                Arguments.of(
                        mockPlanningDistributionInput(
                                DATE_IN_1,
                                DATE_IN_3,
                                DATE_OUT_1,
                                DATE_OUT_3,
                                A_DATE_UTC.toInstant(),
                                true,
                                of(TOT_MONO, TOT_MULTI_BATCH),
                                true
                        ),
                        List.of(
                                new GetPlanningDistributionOutput(DATE_IN_2, DATE_OUT_3, UNITS, TOT_MONO, 44.5)
                        )
                ),
                Arguments.of(
                        mockPlanningDistributionInput(
                                DATE_IN_1,
                                DATE_IN_3,
                                DATE_OUT_1,
                                DATE_OUT_3,
                                A_DATE_UTC.toInstant(),
                                false,
                                of(TOT_MONO, TOT_MULTI_BATCH),
                                true
                        ),
                        List.of(
                                new GetPlanningDistributionOutput(DATE_IN_2, DATE_OUT_3, UNITS, TOT_MONO, 44.5)
                        )
                )
        );
    }

  private GetForecastInput mockForecastInput(final GetPlanningDistributionInput input) {
    return GetForecastInput.builder()
        .workflow(input.getWorkflow())
        .warehouseId(input.getWarehouseId())
        .dateFrom(ofInstant(validateDate(input.getDateOutFrom(), input.getDateInFrom()), UTC))
        .dateTo(ofInstant(validateDate(input.getDateOutTo(), input.getDateInTo()), UTC))
        .viewDate(input.getViewDate())
        .build();
  }

  private Instant validateDate(final Instant dateOut, final Instant dateIn) {
    return dateOut == null ? dateIn : dateOut;
  }

}
