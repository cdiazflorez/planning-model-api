package com.mercadolibre.planning.model.api.usecase.projection.capacity;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.entity.sla.GetSlaByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourService;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.PlanningDistributionService;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.SlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetSlaProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetSlaProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.sla.GetSlaByWarehouseOutboundService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;
import java.util.List;

import static com.mercadolibre.planning.model.api.domain.entity.MetricUnit.UNITS_PER_HOUR;
import static com.mercadolibre.planning.model.api.domain.entity.Workflow.FBM_WMS_OUTBOUND;
import static com.mercadolibre.planning.model.api.util.TestUtils.WAREHOUSE_ID;
import static com.mercadolibre.planning.model.api.web.controller.projection.request.ProjectionType.CPT;
import static java.time.ZonedDateTime.now;
import static java.time.ZonedDateTime.parse;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    private PlanningDistributionService planningService;

    @Mock
    private CalculateCptProjectionUseCase calculateCptProjection;

    @Mock
    private CalculateBacklogProjectionUseCase calculateBacklogProjection;

    @Mock
    private GetCapacityPerHourService getCapacityPerHourService;

    @Mock
    private GetSlaByWarehouseOutboundService getSlaByWarehouseOutboundService;

    @Test
    public void testGetCptProjection() {
        // GIVEN
        final ZonedDateTime etd = parse("2020-01-01T11:00:00Z");
        final ZonedDateTime projectedTime = parse("2020-01-02T10:00:00Z");

        when(calculateCptProjection.execute(any(SlaProjectionInput.class)))
                .thenReturn(List.of(
                        new CptCalculationOutput(etd, projectedTime, 100)));

        when(getCapacityPerHourService.execute(eq(FBM_WMS_OUTBOUND), any(List.class)))
                .thenReturn(List.of(
                        new CapacityOutput(now().withFixedOffsetZone(),
                                UNITS_PER_HOUR, 100)
                ));

        when(getSlaByWarehouseOutboundService.execute(new GetSlaByWarehouseInput(
                WAREHOUSE_ID, DATE_FROM, DATE_TO, emptyList(), TIMEZONE)))
                .thenReturn(emptyList());

        // WHEN
        final List<CptProjectionOutput> result = getSlaProjectionUseCase.execute(getInput());

        // THEN
        verify(planningService).getPlanningDistribution(any(GetPlanningDistributionInput.class));
        verify(getThroughputUseCase).execute(any(GetEntityInput.class));
        verifyNoInteractions(calculateBacklogProjection);

        assertNotNull(result);
        assertEquals(1, result.size());

        final var first = result.get(0);
        assertEquals(etd, first.getDate());
        assertEquals(projectedTime, first.getProjectedEndDate());
        assertEquals(100, first.getRemainingQuantity());
    }

    private GetSlaProjectionInput getInput() {
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
                false
        );
    }
}
