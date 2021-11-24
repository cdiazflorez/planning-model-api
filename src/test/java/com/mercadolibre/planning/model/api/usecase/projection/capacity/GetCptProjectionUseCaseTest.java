package com.mercadolibre.planning.model.api.usecase.projection.capacity;

import com.mercadolibre.planning.model.api.domain.entity.ProcessName;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.CapacityOutput;
import com.mercadolibre.planning.model.api.domain.usecase.capacity.GetCapacityPerHourUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseInput;
import com.mercadolibre.planning.model.api.domain.usecase.cptbywarehouse.GetCptByWarehouseUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.entities.GetEntityInput;
import com.mercadolibre.planning.model.api.domain.usecase.entities.throughput.get.GetThroughputUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionInput;
import com.mercadolibre.planning.model.api.domain.usecase.planningdistribution.get.GetPlanningDistributionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.backlog.calculate.CalculateBacklogProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CalculateCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptCalculationOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionInput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.calculate.cpt.CptProjectionOutput;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.GetCptProjectionUseCase;
import com.mercadolibre.planning.model.api.domain.usecase.projection.capacity.input.GetCptProjectionInput;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCptProjectionUseCaseTest {

    private static final ZonedDateTime DATE_FROM = parse("2020-01-01T12:00:00Z");
    private static final ZonedDateTime DATE_TO = parse("2020-01-10T12:00:00Z");
    private static final String TIMEZONE = "America/Argentina/Buenos_Aires";

    @InjectMocks
    private GetCptProjectionUseCase getCptProjectionUseCase;

    @Mock
    private GetThroughputUseCase getThroughputUseCase;

    @Mock
    private GetPlanningDistributionUseCase getPlanningUseCase;

    @Mock
    private CalculateCptProjectionUseCase calculateCptProjection;

    @Mock
    private CalculateBacklogProjectionUseCase calculateBacklogProjection;

    @Mock
    private GetCapacityPerHourUseCase getCapacityPerHourUseCase;

    @Mock
    private GetCptByWarehouseUseCase getCptByWarehouseUseCase;

    @Test
    public void testGetCptProjection() {
        // GIVEN
        final ZonedDateTime etd = parse("2020-01-01T11:00:00Z");
        final ZonedDateTime projectedTime = parse("2020-01-02T10:00:00Z");

        when(calculateCptProjection.execute(any(CptProjectionInput.class)))
                .thenReturn(List.of(
                        new CptCalculationOutput(etd, projectedTime, 100)));

        when(getCapacityPerHourUseCase.execute(any(List.class)))
                .thenReturn(List.of(
                        new CapacityOutput(now().withFixedOffsetZone(),
                                UNITS_PER_HOUR, 100)
                ));

        when(getCptByWarehouseUseCase.execute(new GetCptByWarehouseInput(
                        WAREHOUSE_ID, DATE_FROM, DATE_TO, emptyList(), TIMEZONE)))
                .thenReturn(emptyList());

        // WHEN
        final List<CptProjectionOutput> result = getCptProjectionUseCase.execute(getInput());

        // THEN
        verify(getPlanningUseCase).execute(any(GetPlanningDistributionInput.class));
        verify(getThroughputUseCase).execute(any(GetEntityInput.class));
        verifyNoInteractions(calculateBacklogProjection);

        assertNotNull(result);
        assertEquals(1, result.size());

        final var first = result.get(0);
        assertEquals(etd, first.getDate());
        assertEquals(projectedTime, first.getProjectedEndDate());
        assertEquals(100, first.getRemainingQuantity());
    }

    private GetCptProjectionInput getInput() {
        return new GetCptProjectionInput(
                FBM_WMS_OUTBOUND,
                WAREHOUSE_ID,
                CPT,
                List.of(ProcessName.PICKING, ProcessName.PACKING),
                DATE_FROM,
                DATE_TO,
                null,
                TIMEZONE,
                false
        );

    }
}
