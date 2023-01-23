package com.mercadolibre.planning.model.api.usecase.backlog;

import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.DATE_IN;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.DATE_OUT;
import static com.mercadolibre.planning.model.api.domain.entity.BacklogGrouper.WORKFLOW;
import static com.mercadolibre.planning.model.api.domain.entity.DeviationType.UNITS;
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
import com.mercadolibre.planning.model.api.domain.entity.LastPhotoRequest;
import com.mercadolibre.planning.model.api.domain.entity.forecast.CurrentForecastDeviation;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.Photo;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.gateway.BacklogGateway;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PlannedBacklogServiceTest {

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
  private BacklogGateway backlogApiGateway;

  @Mock
  private CurrentForecastDeviationRepository currentForecastDeviationRepository;

  @Test
  public void testGetInboundExpectedBacklog() {

    when(backlogApiGateway.getLastPhoto(
            new LastPhotoRequest(
                of(INBOUND, INBOUND_TRANSFER),
                WAREHOUSE_ID,
                of("SCHEDULED"),
                null,
                null,
                null,
                null,
                DATE_11.toInstant(),
                DATE_14.toInstant(),
                of(DATE_IN, DATE_OUT, WORKFLOW),
                DATE_14.toInstant()
            )
        )
    ).thenReturn(
        new Photo(
            DATE_14.toInstant(),
            of(
                new Photo.Group(
                    Map.of(
                        "date_in", "2020-01-01T12:00:00Z",
                        "date_out", "2020-01-01T14:00:00Z",
                        "workflow", "inbound"
                    ),
                    10,
                    10
                ),
                new Photo.Group(
                    Map.of(
                        "date_in", "2020-01-01T13:00:00Z",
                        "date_out", "2020-01-01T14:00:00Z",
                        "workflow", "inbound"
                    ),
                    20,
                    20
                )
            )
        )
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

    when(backlogApiGateway.getLastPhoto(
            new LastPhotoRequest(
                of(INBOUND, INBOUND_TRANSFER),
                WAREHOUSE_ID,
                of("SCHEDULED"),
                null,
                null,
                null,
                null,
                DATE_11.toInstant(),
                DATE_14.toInstant(),
                of(DATE_IN, DATE_OUT, WORKFLOW),
                DATE_14.toInstant()
            )
        )
    ).thenReturn(
        new Photo(
            DATE_14.toInstant(),
            of(
                new Photo.Group(
                    Map.of(
                        "date_in", "2020-01-01T12:00:00Z",
                        "date_out", "2020-01-01T14:00:00Z",
                        "workflow", "inbound-transfer"
                    ),
                    10,
                    10
                ),
                new Photo.Group(
                    Map.of(
                        "date_in", "2020-01-01T12:00:00Z",
                        "date_out", "2020-01-01T14:00:00Z",
                        "workflow", "inbound"
                    ),
                    10,
                    10
                ),
                new Photo.Group(
                    Map.of(
                        "date_in", "2020-01-01T13:00:00Z",
                        "date_out", "2020-01-01T14:00:00Z",
                        "workflow", "inbound"
                    ),
                    20,
                    20
                ),
                new Photo.Group(
                    Map.of(
                        "date_in", "2020-01-01T15:00:00Z",
                        "date_out", "2020-01-01T16:00:00Z",
                        "workflow", "inbound"
                    ),
                    30,
                    30
                )

            )
        )
    );

    when(currentForecastDeviationRepository.findByLogisticCenterIdAndIsActiveTrueAndWorkflowIn(WAREHOUSE_ID,
        Set.of(INBOUND, INBOUND_TRANSFER))
    ).thenReturn(
        of(
            new CurrentForecastDeviation(
                1L, WAREHOUSE_ID, DATE_11, DATE_12, 1.0, true, 10L, INBOUND, DATE_16, DATE_16, UNITS, null),
            new CurrentForecastDeviation(
                1L, WAREHOUSE_ID, DATE_11, DATE_12, 0.5, true, 10L, INBOUND_TRANSFER, DATE_16, DATE_16, UNITS, null)
        )
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
    verifyNoInteractions(backlogApiGateway);

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
}
