package com.mercadolibre.planning.model.api.usecase.backlog;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.INBOUND_TRANSFER;
import static java.time.ZonedDateTime.parse;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mercadolibre.planning.model.api.client.db.repository.forecast.CurrentForecastDeviationRepository;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.util.PlannedBacklogServiceTestUtils;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlannedBacklogServiceTest {

  private static final String WAREHOUSE_ID = "ARBA01";
  private static final ZonedDateTime DATE_11 = parse("2020-01-01T11:00:00Z");
  private static final ZonedDateTime DATE_12 = parse("2020-01-01T12:00:00Z");
  private static final ZonedDateTime DATE_13 = parse("2020-01-01T13:00:00Z");
  private static final ZonedDateTime DATE_14 = parse("2020-01-01T14:00:00Z");
  private static final ZonedDateTime DATE_15 = parse("2020-01-01T15:00:00Z");
  private static final ZonedDateTime DATE_16 = parse("2020-01-01T16:00:00Z");

  @InjectMocks
  private PlannedBacklogService plannedBacklogService;

  @Mock
  private PlanningDistributionService planningDistributionService;

  @Mock
  private PlannedBacklogService.InboundScheduledBacklogGateway inboundScheduledBacklogGateway;

  @Mock
  private CurrentForecastDeviationRepository currentForecastDeviationRepository;

  @Test
  public void testGetInboundScheduledBacklog() {

    when(
        inboundScheduledBacklogGateway.getScheduledBacklog(
            WAREHOUSE_ID,
            DATE_11.toInstant(),
            DATE_14.toInstant(),
            DATE_14.toInstant()
        )
    ).thenReturn(
        PlannedBacklogServiceTestUtils.getPhotoToTestGetInboundScheduledBacklog()
    );

    // WHEN
    final List<PlannedUnits> plannedUnits = plannedBacklogService.getExpectedBacklog(
        WAREHOUSE_ID, FBM_WMS_INBOUND, DATE_11, DATE_14, DATE_14, false
    );

    // THEN
    verifyNoInteractions(planningDistributionService);

    assertNotNull(plannedUnits);
    assertEquals(2, plannedUnits.size());

    final var first = plannedUnits.get(0);
    assertEquals(10, first.getTotal());
    assertEquals(DATE_12, first.getDateIn());
    assertEquals(DATE_14, first.getDateOut());

    final var second = plannedUnits.get(1);
    assertEquals(20, second.getTotal());
    assertEquals(DATE_13, second.getDateIn());
    assertEquals(DATE_14, second.getDateOut());
  }


  @Test
  public void testGetInboundExpectedBacklogWithDeviation() {

    when(
        inboundScheduledBacklogGateway.getScheduledBacklog(
            WAREHOUSE_ID,
            DATE_11.toInstant(),
            DATE_14.toInstant(),
            DATE_14.toInstant()
        )
    ).thenReturn(
        PlannedBacklogServiceTestUtils.getPhotoToTestGetInboundScheduledBacklogWithDeviation()
    );

    when(currentForecastDeviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowIn(WAREHOUSE_ID,
        Set.of(INBOUND, INBOUND_TRANSFER))
    ).thenReturn(
        PlannedBacklogServiceTestUtils.getDeviationsToTestGetInboundExpectedBacklogWithDeviation()
    );

    // WHEN
    final List<PlannedUnits> plannedUnits = plannedBacklogService.getExpectedBacklog(
        WAREHOUSE_ID, FBM_WMS_INBOUND, DATE_11, DATE_14, DATE_14, true
    );

    // THEN
    verifyNoInteractions(planningDistributionService);

    assertNotNull(plannedUnits);
    assertEquals(3, plannedUnits.size());

    final var first = plannedUnits.get(0);
    assertEquals(35, first.getTotal());
    assertEquals(DATE_12, first.getDateIn());
    assertEquals(DATE_14, first.getDateOut());

    final var second = plannedUnits.get(1);
    assertEquals(20, second.getTotal());
    assertEquals(DATE_13, second.getDateIn());
    assertEquals(DATE_14, second.getDateOut());

    final var third = plannedUnits.get(2);
    assertEquals(30, third.getTotal());
    assertEquals(DATE_15, third.getDateIn());
    assertEquals(DATE_16, third.getDateOut());
  }

  @ParameterizedTest
  @MethodSource("argumentsToTestApplyIbTimeAndUnitsDeviations")
  void testApplyTimeAndUnitsDeviationsToInbound(
      final List<CurrentForecastDeviation> deviations,
      final List<PlannedUnits> results
  ) {
    when(
        inboundScheduledBacklogGateway.getScheduledBacklog(
            WAREHOUSE_ID,
            DATE_11.toInstant(),
            DATE_14.toInstant(),
            DATE_14.toInstant()
        )
    ).thenReturn(
        PlannedBacklogServiceTestUtils.getPhotoToTestApplyTimeAndUnitsDeviationsToInbound()
    );
    when(currentForecastDeviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowIn(WAREHOUSE_ID,
        Set.of(INBOUND, INBOUND_TRANSFER))
    ).thenReturn(deviations);

    // WHEN
    final List<PlannedUnits> plannedUnits = plannedBacklogService.getExpectedBacklog(
        WAREHOUSE_ID, FBM_WMS_INBOUND, DATE_11, DATE_14, DATE_14, true
    );
    assertNotNull(plannedUnits);
    assertEquals(results.size(), plannedUnits.size());
    for (int i = 0; i < results.size(); i++) {
      final var result = results.get(i);
      final var planned = plannedUnits.get(i);
      assertEquals(result.getTotal(), planned.getTotal());
      assertEquals(result.getDateIn(), planned.getDateIn());
      assertEquals(result.getDateOut(), planned.getDateOut());
    }
  }

  @Test
  public void testGetOutboundExpectedBacklog() {
    // GIVEN
    when(planningDistributionService.getPlanningDistribution(any(GetPlanningDistributionInput.class)))
        .thenReturn(of(
            GetPlanningDistributionOutput.builder()
                .dateIn(DATE_12)
                .dateOut(DATE_14)
                .total(10)
                .build(),
            GetPlanningDistributionOutput.builder()
                .dateIn(DATE_13)
                .dateOut(DATE_14)
                .total(20)
                .build()
        ));

    // WHEN
    final List<PlannedUnits> plannedUnits = plannedBacklogService.getExpectedBacklog(
        WAREHOUSE_ID, FBM_WMS_OUTBOUND, DATE_11, DATE_14, null, false
    );

    // THEN
    assertNotNull(plannedUnits);
    assertEquals(2, plannedUnits.size());

    final var first = plannedUnits.get(0);
    assertEquals(10, first.getTotal());
    assertEquals(DATE_12, first.getDateIn());
    assertEquals(DATE_14, first.getDateOut());

    final var second = plannedUnits.get(1);
    assertEquals(20, second.getTotal());
    assertEquals(DATE_13, second.getDateIn());
    assertEquals(DATE_14, second.getDateOut());
  }

  private static Stream<Arguments> argumentsToTestApplyIbTimeAndUnitsDeviations() {
    return Stream.of(
        Arguments.of(
            PlannedBacklogServiceTestUtils.onlyUnitsDeviations(),
            PlannedBacklogServiceTestUtils.resultsAfterApplyOnlyUnitDeviations()
        ),
        Arguments.of(
            PlannedBacklogServiceTestUtils.onlyTimeDeviations(),
            PlannedBacklogServiceTestUtils.resultsAfterApplyOnlyTimeDeviations()
        ),
        Arguments.of(
            PlannedBacklogServiceTestUtils.firstTimeAndThenUnitsDeviations(),
            PlannedBacklogServiceTestUtils.resultsAfterApplyFirstTimeAndThenUnitsDeviations()
        ),
        Arguments.of(
            PlannedBacklogServiceTestUtils.firstUnitsAndThenTimeDeviations(),
            PlannedBacklogServiceTestUtils.resultsAfterApplyFirstUnitsAndThenTimeDeviations()
        )
    );
  }
}
