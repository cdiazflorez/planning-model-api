package com.mercadolibre.planning.model.api.usecase.backlog;

import com.mercadolibre.planning.model.api.domain.usecase.backlog.BacklogPhoto;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedBacklogService;
import com.mercadolibre.planning.model.api.domain.usecase.backlog.PlannedUnits;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.gateway.BacklogGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_INBOUND;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static java.time.ZonedDateTime.parse;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PlannedBacklogServiceTest {

    private static final String WAREHOUSE_ID = "ARBA01";

    private static final ZonedDateTime DATE_10 = parse("2020-01-01T10:00:00Z");
    private static final ZonedDateTime DATE_11 = parse("2020-01-01T11:00:00Z");
    private static final ZonedDateTime DATE_12 = parse("2020-01-01T12:00:00Z");
    private static final ZonedDateTime DATE_13 = parse("2020-01-01T13:00:00Z");
    private static final ZonedDateTime DATE_14 = parse("2020-01-01T14:00:00Z");

    @InjectMocks
    private PlannedBacklogService plannedBacklogService;

    @Mock
    private PlanningDistributionService planningDistributionService;

    @Mock
    private BacklogGateway backlogApiGateway;

    private BacklogPhoto buildPhoto(final int total, final Map<String, String> keys) {
        final var photo = new BacklogPhoto();
        photo.setDate(DATE_10.toInstant());
        photo.setKeys(keys);
        photo.setTotal(total);

        return photo;
    }

    @Test
    public void getInboundExpectedBacklog() {
        // GIVEN
        when(backlogApiGateway.getCurrentBacklog(
                WAREHOUSE_ID,
                of(FBM_WMS_INBOUND),
                of("SCHEDULED"),
                DATE_11.toInstant(),
                DATE_14.toInstant(),
                of("date_in", "date_out"))
        ).thenReturn(of(
                buildPhoto(10,
                        Map.of("date_in", "2020-01-01T12:00:00Z",
                                "date_out", "2020-01-01T14:00:00Z")),
                buildPhoto(20,
                        Map.of("date_in", "2020-01-01T13:00:00Z",
                                "date_out", "2020-01-01T14:00:00Z"))
        ));


        // WHEN
        final List<PlannedUnits> plannedUnits = plannedBacklogService.getExpectedBacklog(
                WAREHOUSE_ID, FBM_WMS_INBOUND, DATE_11, DATE_14, false
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
    public void getOutboundExpectedBacklog() {
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
                WAREHOUSE_ID, FBM_WMS_OUTBOUND, DATE_11, DATE_14, false
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
